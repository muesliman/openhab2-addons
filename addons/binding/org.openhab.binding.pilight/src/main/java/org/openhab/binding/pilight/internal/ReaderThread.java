package org.openhab.binding.pilight.internal;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.Socket;

import org.openhab.binding.pilight.internal.IReadCallbacks.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReaderThread extends Thread {
    private enum ReceiveFsm {
        INIT,
        WAIT,
        READY
    }

    private final Logger logger = LoggerFactory.getLogger(ReaderThread.class);

    private String server;
    private int port;
    private IReadCallbacks callbacks;

    private Socket socket = null;
    private PrintStream ws = null;
    private DataInputStream rs = null;

    private int receivedNewLineCnt = 0;
    private final StringBuffer receivedCharBuffer = new StringBuffer();

    private int maxHeartBeatFailedCnt = 5;
    private int heartBeatIntervalMs = 2000;

    private final String heartBeatRequest = "HEART";
    private final String heartBeatResponse = "BEAT";
    private long heartBeatLastTime = System.currentTimeMillis();
    private int heartBeatFailedCnt = 0;

    private boolean threadKill = false;

    /**
     * creates a read thread.
     *
     * @param server
     * @param port
     * @param callbacks
     * @param maxHeartBeatFailedCnt
     * @param heartBeatIntervalMs
     */
    public ReaderThread(String server, int port, IReadCallbacks callbacks, int maxHeartBeatFailedCnt,
            int heartBeatIntervalMs) {
        this(server, port, callbacks);
        this.maxHeartBeatFailedCnt = maxHeartBeatFailedCnt;
        this.heartBeatIntervalMs = heartBeatIntervalMs;
    }

    /**
     * creates a read thread.
     *
     * @param server
     * @param port
     * @param callbacks
     */
    public ReaderThread(String server, int port, IReadCallbacks callbacks) {
        this.server = server;
        this.port = port;
        this.callbacks = callbacks;

        // try to connect. if it fails there is a retry while running (Thread)
        reconnect();
    }

    /**
     * function will terminate the receive thread
     */
    public void stopReading() {
        threadKill = false;
    }

    /**
     * function will send the request on the socket
     *
     * @param request
     */
    public void sendRequest(String request) {
        if (ws == null) {
            logger.warn("socket is n/a. Unable to send request");
        } else {
            logger.trace("send: " + request);
            ws.println(request + "\n");
            ws.flush();
        }
    }

    /**
     * method will close the socket and creates a new socket and streams.
     * In case of an error the socket is closed again.
     * Note: there is no connect
     */
    private void reconnect() {
        closeSocket();

        try {
            heartBeatFailedCnt = 0;
            logger.debug("try to connect");
            socket = new Socket(server, port);
            rs = new DataInputStream(socket.getInputStream());
            ws = new PrintStream(socket.getOutputStream());
            logger.info("connected to server '" + server + ":" + port + "'");
        } catch (IOException e) {
            // socket or stream not created ...
            logger.debug("connect failed\n", e);
            closeSocket();
        }
    }

    /**
     * method will close the socket and catch all related exceptions
     */
    private void closeSocket() {
        if (socket != null) {
            if (socket.isConnected()) {

                if (ws != null) {
                    ws.close();
                }

                if (rs != null) {
                    try {
                        rs.close();
                    } catch (IOException e) {
                        // nothing to do
                    }
                }

                try {
                    socket.close();
                } catch (IOException e) {
                    // nothing to do
                }
            }
        }
        socket = null;
        ws = null;
        rs = null;
        logger.info("disconnected from server. Socket closed.");
    }

    /**
     * Thread entry point
     */
    @Override
    public void run() {
        try {
            receiveThread();
        } catch (InterruptedException e) {
            logger.info("thread interrupted", e);
        }
    }

    /**
     * function to read from stream until there are no more data available
     * if two line ends (\n\n) was found, the content received till this moment
     * is parsed.
     * If the content was a heart beat, the heartBeatFailedCnt will be decremented by '2'
     * In the other case, the content is forward to the parser (from constructor)
     *
     * @return false on error or pilight stopped
     */
    private boolean receiveFromStream() {
        boolean result = true;
        try {
            while (threadKill && (rs.available() > 0)) {
                char ch = (char) rs.readByte();
                if ((receivedCharBuffer.length() == 0) && (ch == '1')) {
                    // exit
                    logger.warn("pilight has stopped");
                    result = false;
                } else {
                    receivedCharBuffer.append(ch);

                    if (ch == '\n') {
                        receivedNewLineCnt++;
                    } else {
                        receivedNewLineCnt = 0;
                    }

                    if (receivedNewLineCnt == 2) {
                        String cmd = receivedCharBuffer.toString().trim();
                        receivedCharBuffer.setLength(0);

                        logger.trace("received: " + cmd);
                        if (cmd.equalsIgnoreCase(heartBeatResponse)) {

                            if (heartBeatFailedCnt > 0) {
                                heartBeatFailedCnt--;
                            }
                            if (heartBeatFailedCnt > 0) {
                                heartBeatFailedCnt--;
                            }
                        } else {
                            callbacks.handleReceivedCommand(cmd);
                        }
                    }
                }
            }
        } catch (IOException e) {
            logger.error("reading stream crashed", e);
            result = false;
        }
        return result;
    }

    /**
     * function to handle heart beat signal
     *
     * therefore a heart beat request is send cyclic.
     * every time a beat was send an internal counter is incremented
     * and every time a beat response was received this counter is decremented by 2
     *
     * when this counter reaches a threshold, this function returns false
     *
     * @see maxHeartBeatIntervalMs
     * @return false in case of the heart beat signal failed. otherwise true
     */
    private boolean handleHeartBeat() {
        boolean result = true;
        if (heartBeatLastTime + heartBeatIntervalMs < System.currentTimeMillis()) {
            heartBeatLastTime = System.currentTimeMillis();
            heartBeatFailedCnt++;
            sendRequest(heartBeatRequest);
            if (heartBeatFailedCnt > maxHeartBeatFailedCnt) {
                // exit
                logger.warn("heartBeatFailed");
                result = false;
            }

        }
        return result;
    }

    /**
     * Main thread to handle the communication.
     * - connect
     * - reconnect
     * - reconnect on heart beat loss
     *
     * @throws InterruptedException
     */
    private void receiveThread() throws InterruptedException {
        ReceiveFsm state = ReceiveFsm.INIT;
        ReceiveFsm prevState = ReceiveFsm.READY; // other to state
        boolean isConnected = false;
        threadKill = true; // Initial FSM shall run

        while (threadKill) {
            if (socket == null) {
                isConnected = false;
            } else {
                isConnected = socket.isConnected();
            }

            switch (state) {
                case INIT:
                    closeSocket();
                    // fall through
                case WAIT:
                    state = ReceiveFsm.WAIT;
                    receivedCharBuffer.setLength(0); // reset all received data
                    if (isConnected) {
                        state = ReceiveFsm.READY;
                    } else {
                        sleep(5000);
                        reconnect();
                    }
                    break;
                case READY:
                    if (isConnected) {
                        if (!receiveFromStream() || !handleHeartBeat()) {
                            state = ReceiveFsm.INIT;
                            // critical
                        }

                    } else {
                        state = ReceiveFsm.WAIT;
                        reconnect();
                    }
                    break;
                default:
                    state = ReceiveFsm.INIT;
                    break;
            }

            if (state != prevState) {
                prevState = state;
                switch (state) {
                    case INIT:
                        callbacks.statusChanged(Status.INIT);
                        break;
                    case READY:
                        callbacks.statusChanged(Status.RUNNING);
                        break;
                    case WAIT:
                        callbacks.statusChanged(Status.WAITING);
                        break;
                    default:
                        callbacks.statusChanged(Status.INIT);
                        break;
                }

            }

        }
        logger.info("reading was regular stopped");
        callbacks.statusChanged(Status.TERMINATED);
    }

}
