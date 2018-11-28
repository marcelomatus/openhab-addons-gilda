package org.openhab.binding.homepilot.handler;

import static org.openhab.binding.homepilot.HomePilotBindingConstants.*;

import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.PercentType;
import org.eclipse.smarthome.core.library.types.StopMoveType;
import org.eclipse.smarthome.core.library.types.UpDownType;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.eclipse.smarthome.core.types.State;
import org.openhab.binding.homepilot.internal.HomePilotDevice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HomePilotThingHandler extends BaseThingHandler {

	private static final Logger logger = LoggerFactory.getLogger(HomePilotThingHandler.class);

	public HomePilotThingHandler(Thing thing) {
		super(thing);
		logger.info("() " + thing);
	}

	@Override
	public void initialize() {
		refresh();
		super.initialize();
	}

	@Override
	public void handleCommand(ChannelUID channelUID, Command command) {
//		logger.info("handleCommand " + channelUID + " - " + command);
		HomePilotBridgeHandler handler = (HomePilotBridgeHandler) getBridge().getHandler();
		Thread.dumpStack();
		if (command instanceof RefreshType) {
			refresh();
		} else {
			switch (channelUID.getId()) {
			case CHANNEL_ROLLERSHUTTER:
				if (command.equals(StopMoveType.STOP)) {
					handler.getGateway().handleStop(getThing().getUID().getId());
					// no state to update here
					// updateState(CHANNEL_POSITION, new PercentType(position));
				} else {

					int position = 0;
					if (command instanceof DecimalType) {
						position = ((DecimalType) command).intValue();
//						logger.info("DecimalType");
					} else if (command instanceof OnOffType) {
//						logger.info("OnOffType");
						position = ((PercentType) ((OnOffType) command).as(PercentType.class)).intValue();
					} else if (command instanceof UpDownType) {
//						logger.info("UpDownType");
						position = ((PercentType) ((UpDownType) command).as(PercentType.class)).intValue();
					} else {
						throw new IllegalStateException("unkown command type " + command.getClass());
					}
//					logger.info("handleSetPosition " + getThing().getUID().getId() + " - " + position);
					position = position > 100 ? 100 : ((position < 0) ? 0 : position);
					handler.getGateway().handleSetPosition(getThing().getUID().getId(), position);
					updateState(CHANNEL_POSITION, new PercentType(position));
				}
				break;
			case CHANNEL_STATE:
				logger.info("handleSetOnOff " + getThing().getUID().getId() + " - " + command);
				handler.getGateway().handleSetOnOff(getThing().getUID().getId(), OnOffType.ON.equals(command));
				updateState(CHANNEL_STATE, OnOffType.ON.equals(command) ? OnOffType.ON : OnOffType.OFF);
				break;
			default:
				throw new IllegalStateException("unknown channel id " + channelUID.getId() + " in " + channelUID);
			}
		}

	}

	@Override
	public void handleUpdate(ChannelUID channelUID, State newState) {
		super.handleUpdate(channelUID, newState);
		//logger.info("handleUpdate " + channelUID + "; " + newState + "; " + getThing().getUID());
	}

	@Override
	public void thingUpdated(Thing thing) {
		super.thingUpdated(thing);
		logger.info("thingUpdated " + thing);
	}

	void refresh() {
		HomePilotBridgeHandler handler = (HomePilotBridgeHandler) getBridge().getHandler();
		HomePilotDevice device = handler.getGateway().loadDevice(getThing().getUID().getId());
		switch (getThing().getThingTypeUID().getId()) {
		case ITEM_TYPE_SWITCH:
			updateState(CHANNEL_STATE, device.getPosition() == 0 ? OnOffType.OFF : OnOffType.ON);
			break;
		case ITEM_TYPE_ROLLERSHUTTER:
			int position = device.getPosition();
			position = position > 100 ? 100 : ((position < 0) ? 0 : position);
			// position must be between 0 and 100
			try {
				updateState(CHANNEL_POSITION, new PercentType(position));
			} catch (Exception e) {
				throw new RuntimeException("pos: '" + position + "'", e);
			}
			break;
		default:
			throw new IllegalStateException(
					"unknown thing type " + getThing().getThingTypeUID().getId() + " in " + getThing());

		}
	}
	//
	// public static void main(String[] args) {
	// new PercentType(0);
	// }
}
