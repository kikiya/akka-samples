package sampe.cqrs;

import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.persistence.AbstractPersistentActor;
import akka.persistence.journal.Tagged;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class SwitchEntity extends AbstractPersistentActor {

    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    private Optional<ImmutableMap<Integer, Boolean>> portStatus = Optional.empty();

    final SettingsImpl settings =
            Settings.SettingsProvider.get(getContext().getSystem());



    @Override
    public Receive createReceiveRecover() {
        return receiveBuilder()
                .match(SwitchEvent.SwitchCreated.class, n -> portStatus = createSwitch(n.n))
                .match(SwitchEvent.PortStatusSet.class, pss -> portStatus = updatePortStatus(pss.port, pss.portEnabled))
                .match(SwitchEvent.PortStatusSent.class, ps -> log.info("i don't really know what to do with this? "+ ps))
                .build();
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(SwitchCommand.CreateSwitch.class, c -> {
                    if(!portStatus.isPresent()) {
                        SwitchEvent switchEvent = new SwitchEvent.SwitchCreated(c.numberOfPorts);
                        persist(switchEvent, (SwitchEvent event) -> {
                            portStatus = createSwitch(c.numberOfPorts);
                            getContext().system().eventStream().publish(event);
                        });
                    } else {
                        log.error("Cannot create a switch that already exists");
                    }
                })
                .match(SwitchCommand.SetPortStatus.class, sp -> {
                    if(portStatus.isPresent() && sp.port >= 0 && sp.port < portStatus.get().size()){
                        SwitchEvent.PortStatusSet switchEvent = new SwitchEvent.PortStatusSet(sp.port, sp.portEnabled);
                        persist(switchEvent, (SwitchEvent event) -> {
                            portStatus = updatePortStatus(sp.port, sp.portEnabled);
                            getContext().system().eventStream().publish(event);
                        });
                    } else {
                        //simplified because java doesn't do pattern matching well
                        log.error("Cannot set port status on non-existing switch or with out of range port");
                    }
                })
                .match(SwitchCommand.SendPortStatus.class, ps -> {
                    if(portStatus.isPresent()){
                        ImmutableSet tags = ImmutableSet.of(eventTag());
                        persist(new Tagged(new SwitchEvent.PortStatusSent(portStatus.get()), tags), (Tagged event) -> {
                            getContext().system().eventStream().publish(event);
                            log.info("persisted {}", event);
                        });
                    } else {
                        log.error("Cannot send port status of non-existing switch");
                    }
                })
                .build();
    }

    @Override
    public String persistenceId() {
        return "SwitchEntity|" + context().self().path().name();
    }

    private String eventTag () {return String.format(settings.tagPrefix + Math.abs(persistenceId().hashCode() % settings.parallelism));}

    private Optional<ImmutableMap<Integer, Boolean>> updatePortStatus(Integer port, Boolean status){
        return portStatus.map(
                ps -> ImmutableMap.<Integer,Boolean>builder().putAll(ps)
                            .put(port, status).build()
        );
    }

    private Optional<ImmutableMap<Integer, Boolean>> createSwitch(int nPorts) {

        Map<Integer, Boolean> switchMap = IntStream.range(0, nPorts).boxed().collect(
                Collectors.toMap( Function.identity(), d -> false));

        System.out.println("Created switch: " + switchMap);

        return Optional.of(ImmutableMap.<Integer, Boolean>builder().putAll(switchMap).build());
    }
}
