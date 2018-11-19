package sampe.cqrs;

import akka.actor.*;

public class CassandraSessionExtension extends AbstractExtensionId<CassandraSessionImpl> implements ExtensionIdProvider {

    public final static CassandraSessionExtension CassandraSessionExtensionProvider = new CassandraSessionExtension();

    private CassandraSessionExtension() {}


    @Override
    public CassandraSessionImpl createExtension(ExtendedActorSystem system) {
        return new CassandraSessionImpl(system);
    }

    @Override
    public ExtensionId<? extends Extension> lookup() {
        return CassandraSessionExtension.CassandraSessionExtensionProvider;
    }
}
