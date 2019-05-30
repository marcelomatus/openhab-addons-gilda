package org.openhab.binding.openthermgateway.internal;

import java.util.HashMap;

public class GatewayCommand {
    private String code;
    private String validationSet;
    private String message;

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return this.message;
    }

    public String getValidationSet() {
        return validationSet;
    }

    public String toFullString() {
        return this.code + "=" + this.message;
    }

    private GatewayCommand(String code, String message, String validationSet) throws Exception {
        this.code = code;
        this.message = message;
        this.validationSet = validationSet;

        if (!validate()) {
            throw new Exception(String.format("Invalid value '%s' for code '%s'", this.message, this.code));
        }
    }

    private boolean validate() {
        if (this.validationSet == null || this.validationSet == "") {
            return true;
        }

        String[] validations = this.validationSet.split(",");

        for (int i = 0; i < validations.length; i++) {
            if (this.message.equals(validations[i])) {
                return true;
            }
        }

        return false;
    }

    public static GatewayCommand parse(String code, String message) throws Exception {
        if ((code == null || code == "") && message.length() > 2 && message.charAt(2) == '=') {
            return parse(message.substring(0, 2), message.substring(3));
        }

        if (code != null && code.length() == 2) {
            String codeUpperCase = code.toUpperCase();

            if (supportedCommands.containsKey(codeUpperCase)) {
                String validateSet = supportedCommands.get(codeUpperCase);
                return new GatewayCommand(codeUpperCase, message, validateSet);
            } else {
                throw new Exception(String.format("Unsupported gateway code '%s'", code.toUpperCase()));
            }
        }

        throw new Exception(
                String.format("Unable to parse gateway command with code '%s' and message '%s'", code, message));
    }

    private static final HashMap<String, String> supportedCommands = getSupportedCommands();

    private static HashMap<String, String> getSupportedCommands() {
        HashMap<String, String> c = new HashMap<String, String>();

        c.put(GatewayCommandCode.TemperatureTemporary, null);
        c.put(GatewayCommandCode.TemperatureConstant, null);
        c.put(GatewayCommandCode.TemperatureOutside, null);
        c.put(GatewayCommandCode.SetClock, null);
        c.put(GatewayCommandCode.HotWater, null);
        c.put(GatewayCommandCode.PrintReport, "A,B,C,G,I,L,M,O,P,R,S,T,V,W");
        c.put(GatewayCommandCode.PrintSummary, "0,1");
        c.put(GatewayCommandCode.GateWay, "0,1,R");
        c.put(GatewayCommandCode.LedA, "R,X,T,B,O,F,H,W,C,E,M,P");
        c.put(GatewayCommandCode.LedB, "R,X,T,B,O,F,H,W,C,E,M,P");
        c.put(GatewayCommandCode.LedC, "R,X,T,B,O,F,H,W,C,E,M,P");
        c.put(GatewayCommandCode.LedD, "R,X,T,B,O,F,H,W,C,E,M,P");
        c.put(GatewayCommandCode.LedE, "R,X,T,B,O,F,H,W,C,E,M,P");
        c.put(GatewayCommandCode.LedF, "R,X,T,B,O,F,H,W,C,E,M,P");
        c.put(GatewayCommandCode.GpioA, "0,1,2,3,4,5,6,7");
        c.put(GatewayCommandCode.GpioB, "0,1,2,3,4,5,6,7");
        c.put(GatewayCommandCode.SetBack, null);
        c.put(GatewayCommandCode.AddAlternative, null);
        c.put(GatewayCommandCode.DeleteAlternative, null);
        c.put(GatewayCommandCode.UnknownID, null);
        c.put(GatewayCommandCode.KnownID, null);
        c.put(GatewayCommandCode.PriorityMessage, null);
        c.put(GatewayCommandCode.SetResponse, null);
        c.put(GatewayCommandCode.ClearResponse, null);
        c.put(GatewayCommandCode.SetpointHeating, null);
        c.put(GatewayCommandCode.SetpointWater, null);
        c.put(GatewayCommandCode.MaximumModulation, null);
        c.put(GatewayCommandCode.ControlSetpoint, null);
        c.put(GatewayCommandCode.CentralHeating, "0,1");
        c.put(GatewayCommandCode.VentilationSetpoint, null);
        c.put(GatewayCommandCode.Reset, null);
        c.put(GatewayCommandCode.IgnoreTransition, "0,1");
        c.put(GatewayCommandCode.OverrideHighbyte, "0,1");
        c.put(GatewayCommandCode.ForceThermostat, "0,1");
        c.put(GatewayCommandCode.VoltageReference, "0,1,2,3,4,5,6,7,8,9");
        c.put(GatewayCommandCode.DebugPointer, null);

        return c;
    }
}