# ClockWork

Bot « à pixels » pour World of Warcraft : l'application **regarde l'écran** et **appuie sur les touches**, sans jamais
toucher au client du jeu (ni lecture de mémoire, ni injection, ni réseau).

Elle fonctionne avec l'addon [ClockWork Addon](https://github.com/tbouchardon/clockwork-addon). L'addon dessine
l'état du jeu sous forme d'une grille de pixels colorés en haut à gauche de l'écran (un « QR code » maison). Cette
application capture la grille, la décode, choisit une action et la joue au clavier et à la souris.

> Le fonctionnement détaillé de la grille, côté jeu, est décrit dans le README de l'addon. Ce document couvre la partie
> Java : capture, perception, décision, action.

---

## Sommaire

1. [Vue d'ensemble](#vue-densemble)
2. [Construire et lancer](#construire-et-lancer)
3. [Utilisation](#utilisation)
4. [La boucle de l'automate](#la-boucle-de-lautomate)
5. [Le cerveau : perception, décision, action](#le-cerveau--perception-décision-action)
6. [Écrire une rotation](#écrire-une-rotation)
7. [Autres activités](#autres-activités)
8. [Organisation du code](#organisation-du-code)
9. [Tests](#tests)
10. [Dépannage](#dépannage)

---

## Vue d'ensemble

```
                    ┌──────────────────────────── ClockWork (Java) ──────────────────────────────┐
  écran de WoW      │                                                                            │
 ┌────────────┐     │  capture 32x32    perception           décision              action        │
 │▣ QR code   │ ──► │  ───────────► QrCodeV2Reader ─► Brain (rotations/*.yaml) ► Automaton ──────┼──► clavier
 │  (0, 23)   │     │                  GameState          + SpellDatabase        (KeyCombo)      │    souris
 └────────────┘     │                  KeyState × 72      (noms → identifiants)                  │
                    └────────────────────────────────────────────────────────────────────────────┘
```

Pourquoi passer par l'écran ? Un addon WoW ne peut **pas** agir seul : lancer un sort exige un clic ou une touche du
joueur. Et depuis la 12.x (Midnight), il ne peut même plus **décider**, car la vie, les temps de recharge et les auras
sont des valeurs « secrètes » qu'il ne peut ni comparer ni calculer. Il peut seulement les afficher. L'addon affiche
donc tout, et c'est ce programme, extérieur au jeu, qui lit les pixels et prend les décisions.

Deux modes de décision coexistent :

| Mode | Activé quand | Qui décide |
|---|---|---|
| **Historique (v1)** | pas de rotation pour la classe et la spécialisation du personnage, ou grille v1 | L'addon allume la touche à appuyer avec une priorité ; Java appuie sur la plus prioritaire. |
| **Cerveau** | une rotation de `rotations/` correspond au personnage, et grille v2/v3 | Java : l'addon décrit l'état de chaque touche, Java applique les règles YAML. |

---

## Construire et lancer

### Prérequis

- **Java 25** : Gradle le télécharge au besoin (toolchain + foojay).
- Les projets voisins, inclus en *composite build* (`settings.gradle.kts`), dans le même dossier parent :

| Dossier | Rôle |
|---|---|
| `../Bot Parent` | Plugins de convention Gradle (`ksuto.*`) et catalogue de versions `libs.versions.toml`. |
| `../Commons` | Utilitaires communs, dont la **capture d'écran** (`fr.ksuto.prh.capture` : `Capture`, `Frame`, `Rgb`). |
| `../Logger` | Journal (SLF4J + Logback). |
| `../Bot Peripherals` | Clavier, souris, écran (`PeripheralRobotHelper`, à base de `java.awt.Robot`). |
| `../Bot Generator` | Plugin `ksuto.picture-enums` : génère des énumérations à partir des images de `src/main/resources`. |

Bibliothèques principales : SnakeYAML (rotation), Apache Commons JEXL (conditions des règles), FlatLaf (thème Swing),
Logback, JUnit.

### Commandes

```bash
./gradlew build          # compile et lance les tests
./gradlew installDist    # produit build/install/clockwork (lib/*.jar + scripts de lancement)
./gradlew run            # lance directement
```

Lancement manuel sous Windows (c'est Windows qui doit capturer l'écran du jeu) :

```bat
java --enable-native-access=ALL-UNNAMED -cp "build\install\clockwork\lib\*" fr.ksuto.clockwork.Runner
```

Le dossier de lancement compte : c'est là que le dossier `rotations/` est cherché.

Propriétés système (`-D…`) :

| Propriété | Défaut | Rôle |
|---|---|---|
| `clockwork.rotations` | `rotations` | Dossier des rotations, choisies selon la classe et la spécialisation du personnage. |
| `clockwork.rotation` | `rotation.yaml` | Rotation imposée quel que soit le personnage, si le fichier existe (tests). |
| `clockwork.wow` | `E:/Perso/World of Warcraft/_retail_` | Dossier du client (`_retail_`, `_classic_era_`…) : produit, version et langue du jeu. |
| `clockwork.cache` | `~/.clockwork/wago` | Cache des tables du jeu téléchargées depuis wago.tools. |

---

## Utilisation

1. Dans WoW, en **fenêtré maximisé**, interface visible, addon chargé : la grille verte apparaît en haut à gauche.
2. Lancer ClockWork : une petite fenêtre toujours au premier plan apparaît en bas à droite de l'écran.
3. Cliquer sur **Auto Config** (bouton engrenage). ClockWork :
   - cherche la grille à l'écran (`QrCode.init`) : quatre coins verts formant un carré de 16x16 ;
   - clique sur la grille pour donner le focus à WoW, puis tape `/clk toggle` si l'addon est désactivé
     (`ensureAddonActive`) ;
   - démarre l'automate (un seul fil d'exécution, même si on relance Auto Config).
4. Le champ de texte affiche les touches jouées. Le journal détaillé (niveau DEBUG pour `fr.ksuto.clockwork`) sort sur
   la console.
5. Le bouton **Pêche** (actif après Auto Config) bascule en mode pêche (voir *Autres activités*).

Le cerveau se pilote depuis les fichiers, à chaud :

- modifier ou ajouter un fichier dans `rotations/` : il est relu dans la seconde ;
- changer de spécialisation en jeu : la rotation correspondante prend le relais, et le journal l'indique.

---

## La boucle de l'automate

`Automaton.play()` tourne jusqu'à la fermeture de la fenêtre. À chaque tour (`searchForSomethingToDo`) :

1. **Capture** de la grille (32x32 pixels à la position trouvée par Auto Config).
2. **Garde-fous**, dans l'ordre :
   - pixel (0,0) non vert → *« QR code invisible »* (WoW masqué, interface cachée, addon non chargé) : on attend ;
   - case `toggle` éteinte → *« addon désactivé »* : on attend ;
   - le changement d'état est journalisé une seule fois, pour savoir **pourquoi** le bot ne fait rien.
3. **Pilote automatique** : ajout ou effacement de points de passage demandés par l'addon ; ramassage du butin à la fin
   d'un combat en mode `drive`.
4. **Choix de la touche** :
   - mode historique : posture à prendre (F1-F5), soins de groupe (ciblage du membre blessé), puis la touche allumée de
     plus haute priorité, avec son modificateur et sa durée d'appui ;
   - **si le cerveau est actif**, sa décision **remplace** celle de l'addon (voir plus bas). Une grille figée (compteur
     de mises à jour immobile depuis plus de 1,5 s : écran de chargement, WoW en arrière-plan) ne produit aucune action.
5. **Action** : appui sur la touche, avec Maj, Ctrl ou Alt si besoin. Sans action et en mode `tne`, appui sur `Tab`
   (cible suivante).
6. **Pause** : 750 ms après une action (temps global de recharge), 200 ms sinon.

---

## Le cerveau : perception, décision, action

Paquet `fr.ksuto.clockwork.brain`. Il est découpé comme un agent classique : ce que l'on voit, ce que l'on décide, ce
que l'on fait.

### Perception : `QrCodeV2Reader` → `GameState`

`QrCodeV2Reader.read(Frame)` lit la version (case (8,13)) et refuse tout ce qui n'est pas v2 ou v3 (retour vide : mode
historique). Il produit un `GameState` immuable :

| Champ | Source |
|---|---|
| `playerHealth`, `playerPower`, `targetHealth`, `targetPower` | Pourcentages, lus dans les cases secrètes côté addon. |
| `hasTarget`, `targetHostile` | Réaction de la cible : noir = aucune, rouge = hostile, jaune = neutre (attaquable aussi, comme le faisait l'addon). Une cible **morte** est grise : présente mais pas hostile. |
| `inCombat`, `casting`, `targetInCombat`, `aggro` | Drapeaux. |
| `enemies` | Ennemis en combat d'après les barres de vie. |
| `facing` | Direction du personnage, en radians. |
| `recommendedSpell` | Sort recommandé par Blizzard (`C_AssistedCombat`), forme de base. |
| `form`, `comboPoints` | Sort de la forme active (0 si aucune), points de combo. |
| `cast` | Sort en cours : identifiant, canalisation, secondes restantes (`Cast.NONE` si aucun). |
| `playerDead`, `mounted`, `targetTapDenied` | Garde-fous : joueur mort, sur une monture, cible marquée par un autre joueur. |
| `classId`, `specId` | Classe et spécialisation du personnage (identifiants du jeu : 7 = chaman, 262 = Élémentaire). |
| `frame` | Compteur de mises à jour (v3). |
| `keys` | 18 touches en v2, **72** en v3 (`1`, `SHIFT-1`, `CTRL-Q`, `ALT-=`…). |

Chaque touche est un `KeyState` :

| Champ | Sens |
|---|---|
| `spellId` | Sort sur la touche (0 = vide). |
| `cooldown` | Temps de recharge restant en secondes (plafonné à 60). |
| `usable` | Utilisable (ressources, conditions). |
| `range` | `IN`, `OUT`, ou `NONE` (portée sans objet : sort sans cible, ou pas de cible). |
| `sinceCastOnTarget` | Secondes depuis le dernier lancement **sur la cible actuelle** (infini si jamais ou plus de 60 s). |
| `sinceCast` | Idem, toutes cibles. |
| `proc` | Bouton en surbrillance. |

`KeyState.ready()` = un sort, utilisable, sans temps de recharge (moins de 0,05 s), pas hors de portée.

Méthodes utiles de `GameState` : `mayAct()` (mode aggro, ou hors combat, ou cible en combat), `keysReady()` (la
grille contient au moins un sort ; elle est vide juste après l'activation), `keyForSpell(id)`.

### Décision : `SpellDatabase`, `Rotation`, `Brain`

- **`SpellDatabase`** traduit les **noms** des règles en **identifiants**, d'après les tables du jeu (voir *Tables du
  jeu* ci-dessous). N'importe quel sort du jeu peut être nommé, sans `/reload`.
  - Les noms sont comparés sans accents, sans casse, apostrophe typographique comprise : « eclair » désigne « Éclair ».
  - Un nom désigne souvent plusieurs identifiants (versions de joueur, de monstre, d'objet) : seuls comptent ceux
    présents sur une touche.
  - `related(id)` relie un sort à ses **variantes** et à ses homonymes. Un talent remplace souvent un sort par une
    variante d'un autre identifiant (ex. Explosion de lave 51505 et sa base 73899, d'après `TraitDefinition`), et
    Blizzard recommande la base.
  - Un identifiant numérique est accepté directement à la place d'un nom.
- **`Rotation`** est le contenu d'un fichier de rotation. Les conditions sont compilées en expressions JEXL au chargement.
  Une erreur de syntaxe est signalée et la rotation précédente est conservée.
- **`Brain.decide(état, rotation, sorts)`** :
  1. rien si la grille n'est pas prête ou si l'on n'a pas le droit d'agir (`mayAct`), et, comme l'addon pour ses
     rotations Lua, **rien sur une monture ou mort** (`busy`) : inutile de l'écrire dans les conditions ;
  2. **sort en cours** : le sort garde la priorité de la règle qui l'a lancé, et seule une règle **strictement plus
     prioritaire** peut agir :
     - **canalisation** (Drain de vie…) : une règle plus prioritaire la coupe (le jeu arrête la canalisation), sa propre
       règle ne la relance pas. Un drain de remplissage cède donc à tout ce qui compte, un drain prioritaire va au bout ;
     - **incantation** (Éclair…) : le jeu refuse les autres sorts. Une règle plus prioritaire l'interrompt d'abord
       selon `stopCasting` (saut par défaut, recul, ou rien pour qui préfère des macros `/stopcasting`), puis lance son
       sort. Sinon, le cerveau attend les dernières 0,4 s, où le sort suivant part en file d'attente (enchaînement sans
       temps mort) ;
     - sort lancé à la main ou inconnu : jamais coupé ;
  3. parcourt les règles par **priorité décroissante**. Une règle s'applique si son sort est sur une touche (n'importe
     quelle combinaison), que la touche est prête (`ready`) et que la condition `when` est vraie ;
  4. la **recommandation de Blizzard**, si `assisted.follow` est vrai, est intercalée à sa priorité : elle passe devant
     les règles moins prioritaires. Elle n'est suivie que contre une **cible ennemie vivante, non marquée par un autre joueur** (`attackableTarget`), et seulement si son sort
     (ou une forme liée) est sur une touche prête ;
  5. renvoie une `Decision` : touche, sort, priorité et raison (journalisée : *« Cerveau : touche SHIFT-R (règle
     « Horion de flamme », priorité 120) »*).

Les conditions JEXL sont **bridées** : elles n'accèdent qu'aux classes du paquet `brain.decision` et aux types de base
(`JexlPermissions.RESTRICTED`), donc pas de réflexion, de fichiers ni de processus. Une règle dont le sort n'est sur
aucune touche est signalée une seule fois.

### Action : `KeyCombo` → `Automaton`

`KeyCombo.parse("SHIFT-R")` sépare la touche et le modificateur. L'automate retrouve la touche physique et l'envoie avec
Maj, Ctrl ou Alt.

### `BrainService`

`BrainService` assemble le tout pour l'automate :

- il relit chaque seconde les fichiers de `rotations/` (`*.yaml`, `*.yml`) qui ont changé ; une rotation invalide est
  signalée et sa version précédente conservée ;
- il **choisit la rotation du personnage** d'après la classe et la spécialisation lues dans la grille : celle de sa
  spécialisation (`class` + `spec`), sinon celle de toute sa classe (`class` sans `spec`). À égalité, le premier fichier
  par ordre alphabétique. Le choix est journalisé à chaque changement (*« Rotation pour SHAMAN Élémentaire : … »*) ;
- sans rotation pour le personnage, il laisse l'addon décider (mode historique) ;
- un `rotation.yaml` dans le dossier de lancement impose sa rotation à tout personnage.

### Tables du jeu (wago.tools)

Paquet `brain.data`. Au premier démarrage du cerveau, un fil d'arrière-plan :

1. identifie le client (`GameInstall`) : le **produit** dans `.flavor.info` du dossier du client (`wow` pour retail,
   `wow_classic_era` pour vanilla…), sa **version** dans `.build.info` (dossier parent) et la **langue** dans
   `WTF/Config.wtf` (`textLocale`) ;
2. télécharge les tables DB2 au format CSV depuis wago.tools, une seule fois par produit, version et langue
   (`WagoTables`, cache `<clockwork.cache>/<produit>/<version>/<langue>/`) : environ 15 Mo, une dizaine de secondes.
   Seule `SpellName` est indispensable : une table absente de la version (talents en vanilla) est traitée comme vide ;
3. construit `SpellDatabase` : tous les sorts du jeu (`SpellName`, plus de 400 000), et les sorts **de chaque classe**,
   c'est-à-dire capacités de classe (`SkillLine`, `SkillLineAbility`), de spécialisation (`SpecializationSpells`) et
   talents, avec les variantes qu'ils accordent (`SkillLineXTraitTree` → `TraitNode` → `TraitNodeEntry` →
   `TraitDefinition`) : environ 300 noms par classe ;
4. génère `rotation.spells.json` dans le dossier de lancement : spécialisations de chaque classe, et sorts de chaque
   classe et de chaque spécialisation (voir *Autocomplétion dans l'éditeur*).

Le cerveau attend que la table soit chargée pour décider (*« Cerveau en attente de la table des sorts »*).

Le chargement (`SpellDatabaseLoader`) a des replis :

| Situation | Comportement |
|---|---|
| Version connue de wago.tools | Téléchargée (ou lue en cache). Les **autres versions du même produit** sont ensuite supprimées du cache ; retail et vanilla gardent chacun le leur. |
| Version inconnue (client de serveur privé modifié, version sortie dans l'heure) | Version connue la plus proche du même produit (`/api/builds`) : la plus récente qui ne dépasse pas la nôtre, sinon la plus ancienne. Les noms de sorts changent très peu d'une version à l'autre. |
| Pas de réseau | Version la plus récente entièrement en cache pour ce produit et cette langue. |
| Rien de tout cela | Seuls les identifiants numériques sont utilisables dans les règles. |

Une mise à jour du client par le launcher change la version : au démarrage suivant, les tables sont retéléchargées une
fois. Pour un autre client (vanilla, Forever…), il suffit de pointer `clockwork.wow` vers son dossier
(`…/_classic_era_`).

---

## Écrire une rotation

Une rotation est un fichier YAML du dossier `rotations/`, par exemple `rotations/chaman-elementaire.yaml` :

```yaml
# yaml-language-server: $schema=../rotation.schema.json
name: Chaman Élémentaire
class: SHAMAN
spec: Élémentaire

# Recommandation de Blizzard en repli : elle passe devant les règles de priorité inférieure
assisted:
  follow: true
  priority: 50

rules:
  - cast: Afflux de soins
    when: "player.health < 40"
    priority: 200

  # Horion de flamme dure 18 s : on le réapplique sur la cible après 13 s
  - cast: Horion de flamme
    when: "target.hostile && spell.sinceCastOnTarget('Horion de flamme') > 13"
    priority: 120

  - cast: Éclair
    when: "target.hostile"
    priority: 10
```

- `name` : nom libre, affiché dans le journal.
- `stopCasting` : façon d'interrompre sa propre incantation quand une règle plus prioritaire s'applique : `jump` (saut,
  par défaut : le personnage reste en place, seuls les sorts instantanés partent pendant le saut), `back` (petit recul,
  immédiat mais le personnage bouge un peu), `none` (rien : sorts utilisables pendant l'incantation, macros
  `/stopcasting`).
- `class` : classe visée (`SHAMAN`, `MAGE`…).
- `spec` : spécialisation visée, nom affiché en jeu (`Élémentaire`, `Farouche`…) ou identifiant (`262`). Sans `spec`, la
  rotation vaut pour toutes les spécialisations de la classe ; une rotation de la spécialisation passe devant.
- `cast` : nom du sort (tel qu'affiché en jeu, accents et casse indifférents) ou identifiant.
- `when` : condition JEXL (`&&`, `||`, `!`, `<`, `>`, `==`, arithmétique). Si elle est absente, la règle est toujours
  vraie.
- `priority` : la plus haute l'emporte.

Variables disponibles dans `when` :

| Variable | Sens |
|---|---|
| `player.health`, `player.power` | Pourcentages. |
| `player.combat`, `player.casting`, `player.aggro` | Booléens. |
| `player.castSpell`, `player.channeling`, `player.castRemaining` | Sort en cours (`''` si aucun), canalisation ou incantation, secondes restantes. Ex. : ne couper un drain qu'en fin de canalisation. |
| `player.form` | Nom de la forme active (druide…), `''` sans forme. |
| `player.combo` | Points de combo. |
| `target.exists`, `target.hostile`, `target.combat` | Booléens. `hostile` = cible attaquable : ennemie ou neutre (rouge ou jaune), **vivante**, non marquée par un autre joueur. |
| `target.health`, `target.power` | Pourcentages. |
| `enemies` | Nombre d'ennemis en combat à proximité. |
| `assisted` | Nom du sort recommandé par Blizzard. |
| `spell.ready('Nom')`, `spell.usable('Nom')`, `spell.inRange('Nom')`, `spell.proc('Nom')`, `spell.onBar('Nom')` | Booléens. |
| `spell.cooldown('Nom')` | Secondes de recharge restantes. |
| `spell.form('Nom')` | Le personnage est sous cette forme (accents, casse et apostrophe indifférents). |
| `spell.sinceCast('Nom')`, `spell.sinceCastOnTarget('Nom')` | Secondes depuis le dernier lancement (toutes cibles / cible actuelle), infini au-delà de 60 s. |

### Autocomplétion dans l'éditeur

La première ligne associe le fichier au **schéma JSON** `rotation.schema.json` (versionné). IntelliJ la reconnaît
nativement, VS Code avec l'extension YAML de Red Hat. On obtient l'autocomplétion des clés, la validation à la frappe
(clé inconnue, type faux, règle sans `cast`) et la documentation au survol, dont la liste des variables de `when`.

Le schéma est **conditionnel** (`if` / `then`) et renvoie à `rotation.spells.json`, généré par ClockWork d'après les
tables du jeu (voir *Tables du jeu*) :

- selon `class`, l'éditeur propose les spécialisations de la classe pour `spec` ;
- selon `class` et `spec`, il propose pour `cast` les sorts **de la spécialisation** : sorts de toute la classe, plus
  ceux qui lui sont réservés. Par exemple, pour un chaman, Horion de terre n'est proposé qu'en Élémentaire, et
  Frappe-tempête qu'en Amélioration. Sans `spec`, ce sont les sorts de toute la classe ;
- un nom inconnu, ou réservé à une autre spécialisation, est signalé.

Les sorts réservés à une spécialisation viennent de `SpecializationSpells` et des nœuds de talents soumis à une condition
de spécialisation (`TraitCond`, `SpecSetMember`). Les composantes d'autres sorts (dégâts de Frappe-tempête, attaque de la
main gauche…) sont écartées. Le contenu de `when` reste du texte libre pour le schéma : une erreur JEXL est
signalée par ClockWork au chargement.

Le temps depuis le dernier lancement **sur la cible** est le substitut aux debuffs : les auras sont illisibles en combat
depuis la 12.x.

Limites actuelles :

- touches décrites : `1`-`0`, `)`, `=`, `Q D R T F G`, seules ou avec un modificateur ;
- pas d'état entre deux décisions (séquences, variables).

### Rotations fournies

| Fichier | Contenu |
|---|---|
| `rotations/chaman-elementaire.yaml` | Chaman Élémentaire (exemple de départ). |
| `rotations/druide.yaml` | Druide, portage de la rotation Lua historique : règles par forme (lanceur/sélénien, ours, félin), Éclat lunaire entretenu, Morsure féroce selon les points de combo. |

Dans `when`, un nom contenant une apostrophe s'écrit avec l'apostrophe typographique (`'Forme d’ours'`), puisque les
noms sont entre apostrophes droites ; ClockWork confond les deux.

---

## Autres activités

Ces fonctions viennent des versions précédentes et sont toujours en place :

- **Pêche** (`Fisherman`) : pose le leurre (`W`) toutes les 10 min et l'appât (`Maj+W`) toutes les 5 min, lance la
  ligne (`H`), repère le bouchon dans une zone devant le personnage (`ShowZone`) et clique dès qu'il bouge. Bouger la
  souris arrête la pêche.
- **Pilote automatique** (`TomTom`) : lit les coordonnées de carte codées en binaire dans la grille, suit une liste de
  points de passage, se dégage quand il est bloqué (recul, saut, rotation). Il s'arrête pour combattre, sous 50 % de vie
  et pendant un repas.
- **Frappé sans riposter** (`HitDetector`) : en pilote automatique, si la vie du joueur a baissé d'au moins 2 points ces
  6 dernières secondes alors que le bot n'a appuyé sur aucune touche, c'est qu'un monstre non ciblé le frappe (souvent
  dans le dos) : demi-tour, puis `Tab` le cible. La vie est secrète pour l'addon mais lue en clair par le Java ; le
  journal de combat, qui servait autrefois, est interdit en 12.x.
- **Soins de groupe** : quand l'addon signale un membre blessé, ciblage par `Maj+F2…F5` (groupe) ou `Alt+Maj+lettre`
  (raid), puis la rotation soigne.
- **Ramassage du butin** après chaque combat en mode `drive`.

---

## Organisation du code

```
src/main/java/fr/ksuto/clockwork/
├── Runner.java                 point d'entrée : thème FlatLaf, injection Guice, fenêtre
├── ClockWorkUI.java            fenêtre (Auto Config, Pêche, journal), démarre l'automate
├── activity/
│   ├── Automaton.java          boucle principale : capture → décision → touche
│   ├── Fisherman.java          pêche
│   ├── TomTom.java             pilote automatique
│   └── Healer.java             (historique)
├── brain/
│   ├── BrainService.java       rechargement à chaud de la rotation, chargement de la table des sorts
│   ├── data/                   GameInstall, WagoTables, SpellDatabaseLoader, SpellDatabase, SpellSchema, Csv
│   ├── perception/             QrCodeV2Reader, GameState, KeyState, KeyCombo
│   └── decision/               Brain, Rotation, SpellView
├── entities/
│   ├── qrcode/                 QrCode (recherche à l'écran, cases v1), Dot, Key, ComplexKey
│   ├── Player.java, ClkPosition.java
└── tools/ShowZone.java         affichage d'une zone de l'écran (pêche)
```

La disposition des cases doit rester **identique** des deux côtés : `QrCodeV2Reader` (Java) et `qrcode_v2.lua` (addon)
partagent l'ordre des touches (`KEY_ORDER`), les blocs (`BLOCKS` / `QR_BLOCKS`) et les fonctions de position
(`stateCell`, `historyCell`, `spellCell` / `spellIdCell`).

---

## Tests

```bash
./gradlew test
```

- `QrCodeV2ReaderTest` : décodage d'une grille synthétique, v2 et v3 (blocs à modificateurs, compteur, cible morte).
- `BrainTest` : priorités, conditions, recommandation de Blizzard (formes liées, cible requise), garde-fous.
- `SpellDatabaseTest` : CSV de wago.tools, noms sans accents, variantes, sorts par classe (talents compris), produit,
  version et langue du jeu, liste pour l'éditeur.
- `DruidRotationTest` : `rotations/druide.yaml`, une décision par forme.
- `BrainServiceTest` : choix de la rotation selon la classe et la spécialisation, rotation imposée.
- `HitDetectorTest` : perte de vie sans action, seuil, régénération, fenêtre glissante.
- `SpellDatabaseLoaderTest` : replis (version proche, cache), nettoyage du cache par produit, comparaison de versions.

---

## Dépannage

| Symptôme | Cause probable |
|---|---|
| *« QR code invisible »* | WoW minimisé ou recouvert, interface masquée (`Alt+Z`), addon non chargé. |
| *« addon désactivé »* | `/clk toggle` en jeu (Auto Config le fait normalement). |
| *« grille figée »* | Écran de chargement, WoW en pause ou en arrière-plan. |
| *« Cerveau inactif : grille v1 »* | Addon trop ancien : redéployer l'addon et `/reload`. |
| *« Règle ignorée : … n'est sur aucune touche »* | Sort absent des touches décrites, ou nom inconnu : vérifier le nom (l'éditeur le signale avec `class` et `spec`). |
| *« Aucune rotation pour … : l'addon décide seul »* | Pas de fichier dans `rotations/` pour cette classe et cette spécialisation. |
| Auto Config ne trouve rien | Grille déformée : WoW doit être en fenêtré maximisé ; l'addon recalcule l'échelle des pixels à chaque changement de taille. |
| `installDist` échoue (fichier verrouillé) | ClockWork tourne encore et verrouille ses `.jar` : l'arrêter avant de construire. |
| Gradle ne trouve pas Java sous WSL | `JAVA_HOME` pointe vers un JDK Windows : utiliser un JDK Linux (`export JAVA_HOME=…`). |
