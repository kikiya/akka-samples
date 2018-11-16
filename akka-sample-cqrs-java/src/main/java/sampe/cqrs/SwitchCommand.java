package sampe.cqrs;

public interface SwitchCommand {

    final class CreateSwitch implements SwitchCommand {

        public final int numberOfPorts;

        public CreateSwitch(int numberOfPorts) {
            this.numberOfPorts = numberOfPorts;
        }

    }

    final class SetPortStatus implements SwitchCommand {
        public final int port;
        public final boolean portEnabled;

        public SetPortStatus(int port, boolean portEnabled){
            this.port = port;
            this.portEnabled = portEnabled;
        }
    }

    final class SendPortStatus implements SwitchCommand{}
}
