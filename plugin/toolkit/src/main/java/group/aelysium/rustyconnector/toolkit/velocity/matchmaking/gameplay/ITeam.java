package group.aelysium.rustyconnector.toolkit.velocity.matchmaking.gameplay;

import group.aelysium.rustyconnector.toolkit.velocity.matchmaking.storage.IRankedPlayer;
import group.aelysium.rustyconnector.toolkit.velocity.matchmaking.storage.player_rank.IPlayerRank;
import group.aelysium.rustyconnector.toolkit.velocity.players.IPlayer;

import java.util.List;

public interface ITeam<TPlayer extends IPlayer, TRankedPlayer extends IRankedPlayer<TPlayer, ? extends IPlayerRank<?>>> {
    /**
     * Adds a player to the team.
     * @param player The player to add.
     * @return `true` if the player was added successfully. `false` otherwise.
     */
    boolean add(TRankedPlayer player);

    /**
     * Gets all the players in this team.
     * @return {@link List<IPlayer>}
     */
    List<TRankedPlayer> players();

    /**
     * Tells if the team contain the bare minimum of players required.
     * @return `true` if there are enough players for this team. `false` otherwise.
     */
    boolean satisfactory();

    /**
     * Tells if the team contains the maximum number of players it's allowed.
     * @return `true` if the team is full. `false` otherwise.
     */
    boolean full();

    record Settings(int min, int max) {}
}
