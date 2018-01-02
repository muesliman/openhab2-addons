package org.openhab.binding.pilight;

public class PilightGatewayConfig {
    public String ipAddress;
    public int port;

    public boolean heartbeatCheck;
    public int heartbeatFrequence;
    public int updadeInverval;
    public int configUpdadeInverval;

    @Override
    public String toString() {
        return "PilightGatewayConfig [ipAddress=" + ipAddress + ", port=" + port + ", heartbeatCheck=" + heartbeatCheck
                + ", heartbeatFrequence=" + heartbeatFrequence + ", updadeInverval=" + updadeInverval
                + ", configUpdadeInverval=" + configUpdadeInverval + "]";
    }

}
