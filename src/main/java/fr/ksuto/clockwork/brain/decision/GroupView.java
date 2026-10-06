package fr.ksuto.clockwork.brain.decision;

import fr.ksuto.clockwork.brain.perception.Group;

import java.util.stream.Stream;

/**
 * Groupe dans les conditions JEXL : {@code group.below(80) >= 3}, {@code group.size}. Les membres morts ou déconnectés
 * ne comptent pas.
 */
public final class GroupView {

    private final Group group;

    GroupView(Group group) {

        this.group = group;
    }

    private Stream<Group.Member> alive() {

        return group.members().stream().filter(member -> !member.dead() && !member.offline());
    }

    /**
     * Membres vivants et connectés, joueur compris.
     */
    public int getSize() {

        return (int) alive().count();
    }

    /**
     * Nombre de membres vivants sous ce pourcentage de vie.
     */
    public int below(double health) {

        return (int) alive().filter(member -> member.health() < health).count();
    }

    /**
     * Vie moyenne des membres vivants, en % (100 sans groupe).
     */
    public double getAvgHealth() {

        return alive().mapToDouble(Group.Member::health).average().orElse(100);
    }
}
