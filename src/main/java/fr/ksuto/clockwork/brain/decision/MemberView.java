package fr.ksuto.clockwork.brain.decision;

import fr.ksuto.clockwork.brain.data.SpellDatabase;
import fr.ksuto.clockwork.brain.perception.Group;

import java.util.Map;
import java.util.Objects;

/**
 * Membre visé par une règle {@code on} dans les conditions JEXL : {@code member.health < 50},
 * {@code member.sinceCast('Récupération') > 12}.
 */
public final class MemberView {

    private final Group.Member       member;
    private final SpellDatabase      database;
    private final Map<Integer, Long> casts;
    private final long               now;

    /**
     * @param casts derniers lancements sur ce membre (identifiant du sort → instant en ms), suivis par le cerveau
     */
    MemberView(Group.Member member, SpellDatabase database, Map<Integer, Long> casts, long now) {

        this.member = member;
        this.database = database;
        this.casts = casts;
        this.now = now;
    }

    public double getHealth() {

        return member.health();
    }

    /**
     * Rôle : {@code 'tank'}, {@code 'healer'}, {@code 'damager'}, ou {@code ''} si aucun.
     */
    public String getRole() {

        return member.role() == Group.Role.NONE ? "" : member.role().name().toLowerCase();
    }

    public boolean isTank() {

        return member.role() == Group.Role.TANK;
    }

    public boolean isHealer() {

        return member.role() == Group.Role.HEALER;
    }

    /**
     * C'est le joueur lui-même : {@code member.self}.
     */
    public boolean isSelf() {

        return member.self();
    }

    /**
     * Secondes depuis le dernier lancement du sort sur ce membre par le cerveau, infini si jamais ou plus de 60 s.
     * Les lancements à la main ne sont pas comptés.
     */
    public double sinceCast(String reference) {

        double since = database.idsFor(reference).stream()
                               .map(casts::get)
                               .filter(Objects::nonNull)
                               .mapToDouble(time -> (now - time) / 1000.0)
                               .min().orElse(Double.POSITIVE_INFINITY);
        return since > 60 ? Double.POSITIVE_INFINITY : since;
    }
}
