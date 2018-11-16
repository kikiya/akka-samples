package sampe.cqrs;

import com.google.common.collect.ImmutableMap;

public interface SwitchEvent {

    final class SwitchCreated implements SwitchEvent {
        public final int n;

        public SwitchCreated(int n){
            this.n = n;
        }
    }

    final class PortStatusSet implements SwitchEvent {
        public final int port;
        public final boolean portEnabled;

        public PortStatusSet(int port, boolean portEnabled){
            this.port = port;
            this.portEnabled = portEnabled;
        }
    }

    final class PortStatusSent implements SwitchEvent {
        public final ImmutableMap<Integer, Boolean> portStatus;

        public PortStatusSent(ImmutableMap<Integer, Boolean> portStatus){
            this.portStatus = portStatus;
        }
    }

}
