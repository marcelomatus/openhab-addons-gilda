package org.openhab.binding.supla.internal.api;

import com.google.gson.reflect.TypeToken;
import org.openhab.binding.supla.internal.server.SuplaIoDevice;
import org.openhab.binding.supla.internal.server.SuplaIoDevices;
import org.openhab.binding.supla.internal.server.SuplaToken;
import org.openhab.binding.supla.internal.server.http.CommonHeaders;
import org.openhab.binding.supla.internal.server.http.HttpExecutor;
import org.openhab.binding.supla.internal.server.http.Request;
import org.openhab.binding.supla.internal.server.http.Response;
import org.openhab.binding.supla.internal.server.mappers.JsonMapper;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

public final class SuplaIoDevicesManager implements IoDevicesManager {
    private static final Type MAP_TYPE = new TypeToken<Map<String, List<SuplaIoDevice>>>(){}.getType();
    private static final Type IO_DEVICES_TYPE = new TypeToken<List<SuplaIoDevice>>(){}.getType();
    private static final String KEY_FOR_IO_DEVICES = "iodevices";

    private final TokenManager tokenManager;
    private final HttpExecutor httpExecutor;
    private final JsonMapper jsonMapper;

    public SuplaIoDevicesManager(TokenManager tokenManager, HttpExecutor httpExecutor, JsonMapper jsonMapper) {
        this.tokenManager = checkNotNull(tokenManager);
        this.httpExecutor = checkNotNull(httpExecutor);
        this.jsonMapper = checkNotNull(jsonMapper);
    }

    @Override
    public List<SuplaIoDevice> obtainIoDevices() {
        final Optional<SuplaToken> token = tokenManager.obtainToken();

        final Response response = httpExecutor.get(new Request("/api/iodevices", CommonHeaders.AUTHORIZATION_HEADER(token.get())));

        System.out.println("response:\n" + response.getResponse());

        final Map<String, List<SuplaIoDevice>> map = jsonMapper.to(MAP_TYPE, response.getResponse());

        return map.get(KEY_FOR_IO_DEVICES);
    }
}
