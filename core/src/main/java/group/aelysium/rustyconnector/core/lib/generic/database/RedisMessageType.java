package group.aelysium.rustyconnector.core.lib.generic.database;

public enum RedisMessageType {
    /**
     * Regarding messages, sub-servers never have any way of knowing if the Proxy is actually online or listening.
     * They simply send messages into the Redis Channel in hopes that the proxy is listening.
     *
     * This is done so that we don't have the proxy's IP Address stored on the sub-servers.
     * Instead, it's the proxy's job to keep track of the IP Addresses of all sub-servers and to recognize who is speaking to it.
     * When the proxy needs to send a message to a specific server, it will include a `to` parameter which specifies which
     * sub-server is allowed to read it.
     * The sub-servers don't have a `to` parameter and instead shoot their messages into the data-channel blindly.
     *
     * If a message contains the `from` parameter it is from a sub-server.
     * If a message contains the `to` parameter it is from the proxy.
     * Messages cannot contain both `to` and `from` parameters. Additionally, these parameters cannot be set manually.
     */

    PING,
    PONG,
    REQ_REG, // Proxy > Server | An outbound request for servers to register themselves
    REG, // Server > Proxy | A server's response to the REG_OUT message. This is also used when a server boots up and needs to register itself.
    UNREG, // Server > Proxy | A server's message to the proxy when it needs to un-register itself.
    PLAYER_CNT, // Server > Proxy | A player count update from the server to the proxy
    PLAYER_DISCON, // Server > Proxy | When a player disconnects from the network

}
