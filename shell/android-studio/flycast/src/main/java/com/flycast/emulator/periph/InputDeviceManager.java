package com.flycast.emulator.periph;

import android.content.Context;
import android.hardware.display.DisplayManager;
import android.hardware.input.InputManager;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.InputDevice;
import android.view.KeyEvent;

import com.flycast.emulator.Emulator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.ArrayUtils;

public final class InputDeviceManager implements InputManager.InputDeviceListener {
    public static final int VIRTUAL_GAMEPAD_ID = 0x12345678;

    static { System.loadLibrary("flycast"); }
    private static final InputDeviceManager INSTANCE = new InputDeviceManager();
    private InputManager inputManager;
    private DisplayManager displayManager;

    private static final int PORT_COUNT = 4;
    // The handheld's own pad, which gives port A up to any pad that joins
    private static final String BUILT_IN_CONTROLLER_NAME = "Odin Controller";
    // Android id of the pad playing on each maple port, 0 if none
    private final int[] portHolders = new int[PORT_COUNT];
    private final Set<Integer> pads = new HashSet<>();

    // Leaving the dock hands port A back to the handheld's own pad, as on a Switch
    private final DisplayManager.DisplayListener displayListener = new DisplayManager.DisplayListener() {
        @Override
        public void onDisplayAdded(int displayId) {
        }

        @Override
        public void onDisplayRemoved(int displayId) {
            if (displayManager != null
                    && displayManager.getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION).length == 0) {
                int builtIn = findBuiltIn();
                if (builtIn != 0 && createDevice(builtIn) && portHolders[0] != builtIn)
                    assignPort(0, builtIn);
            }
        }

        @Override
        public void onDisplayChanged(int displayId) {
        }
    };

    private boolean hasTouchscreen = false;

    private static class VibrationParams {
        float power;
        float inclination;
        long stopTime;
    }
    private Map<Integer, VibrationParams> vibParams = new HashMap<>();
    private Set<Integer> knownDevices = new HashSet<>();

    public InputDeviceManager()
    {
        init();
    }

    public void startListening(Context applicationContext)
    {
        knownDevices.clear();
        pads.clear();
        Arrays.fill(portHolders, 0);
        hasTouchscreen = applicationContext.getPackageManager().hasSystemFeature("android.hardware.touchscreen");
        if (hasTouchscreen)
            joystickAdded(VIRTUAL_GAMEPAD_ID, null, 0, null,
                    null, null, getVibrator(VIRTUAL_GAMEPAD_ID) != null);
        inputManager = (InputManager)applicationContext.getSystemService(Context.INPUT_SERVICE);
        inputManager.registerInputDeviceListener(this, null);
        displayManager = (DisplayManager)applicationContext.getSystemService(Context.DISPLAY_SERVICE);
        displayManager.registerDisplayListener(displayListener, null);
    }

    public void stopListening()
    {
        if (inputManager != null) {
            inputManager.unregisterInputDeviceListener(this);
            inputManager = null;
        }
        if (displayManager != null) {
            displayManager.unregisterDisplayListener(displayListener);
            displayManager = null;
        }
        joystickRemoved(VIRTUAL_GAMEPAD_ID);
    }

    @Override
    public void onInputDeviceAdded(int i) {
    }

    @Override
    public void onInputDeviceRemoved(int i) {
        joystickRemoved(i);
        knownDevices.remove(i);
        pads.remove(i);
        for (int port = 0; port < PORT_COUNT; port++)
            if (portHolders[port] == i)
                portHolders[port] = 0;
        // The handheld's own pad takes port A back when its pad went away
        if (portHolders[0] == 0) {
            for (int id : pads)
                if (isBuiltIn(id)) {
                    assignPort(0, id);
                    break;
                }
        }
    }

    @Override
    public void onInputDeviceChanged(int i) {
    }

    private Vibrator getVibrator(int i) {
        if (i == VIRTUAL_GAMEPAD_ID) {
            return (Vibrator)Emulator.getAppContext().getSystemService(Context.VIBRATOR_SERVICE);
        }
        else {
            InputDevice device = InputDevice.getDevice(i);
            if (device == null)
                return null;
            Vibrator vibrator = device.getVibrator();
            return vibrator.hasVibrator() ? vibrator : null;
        }
    }

    private void vibrate(Vibrator vibrator, long duration_ms, float power)
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int ipow = Math.min((int)(power * 255), 255);
            if (ipow >= 1)
                vibrator.vibrate(VibrationEffect.createOneShot(duration_ms, ipow));
            else
                vibrator.cancel();
        }
        else
            vibrator.vibrate(duration_ms);
    }

    // Called from native code
    // returns false if the device has no vibrator
    private boolean rumble(int i, float power, float inclination, int duration_ms)
    {
        Vibrator vibrator = getVibrator(i);
        if (vibrator == null)
            return false;
        if (i == VIRTUAL_GAMEPAD_ID) {
            if (Emulator.vibrationPower == 0)
                return true;
            power *= Emulator.vibrationPower / 100.f;
        }

        VibrationParams params;
        synchronized (this) {
            params = vibParams.get(i);
            if (params == null) {
                params = new VibrationParams();
                vibParams.put(i, params);
            }
        }
        if (power != 0) {
            params.stopTime = System.currentTimeMillis() + duration_ms;
            if (inclination > 0)
                params.inclination = inclination * power;
            else
                params.inclination = 0;
        }
        params.power = power;
        VibratorThread.getInstance().setVibrating();

        return true;
    }

    public boolean updateRumble()
    {
        List<Integer> ids;
        synchronized (this) {
            ids = new ArrayList<Integer>(vibParams.keySet());
        }
        boolean active = false;
        for (int id : ids) {
            if (updateRumble(id))
                active = true;
        }
        return active;
    }

    private boolean updateRumble(int i)
    {
        Vibrator vibrator = getVibrator(i);
        VibrationParams params;
        synchronized (this) {
            params = vibParams.get(i);
        }
        if (vibrator == null || params == null)
            return false;
        long remTime = params.stopTime - System.currentTimeMillis();
        if (remTime <= 0 || params.power == 0) {
            params.power = 0;
            params.inclination = 0;
            vibrator.cancel();
            return false;
        }
        if (params.inclination > 0)
            vibrate(vibrator, remTime, params.inclination * remTime);
        else
            vibrate(vibrator, remTime, params.power);
        return true;
    }

    public void stopRumble()
    {
        List<Integer> ids;
        synchronized (this) {
            ids = new ArrayList<Integer>(vibParams.keySet());
        }
        for (int id : ids) {
            Vibrator vibrator = getVibrator(id);
            if (vibrator != null)
                vibrator.cancel();
        }
    }

    public boolean hasTouchscreen() {
        return hasTouchscreen;
    }

    private boolean createDevice(int id)
    {
        if (id == 0)
            return false;
        if (knownDevices.contains(id))
            return true;
        InputDevice device = InputDevice.getDevice(id);
        if (device == null || (device.getSources() & InputDevice.SOURCE_CLASS_BUTTON) != InputDevice.SOURCE_CLASS_BUTTON)
            return false;
        // Pads play on no port until they join with a button press
        boolean pad = (device.getSources() & InputDevice.SOURCE_CLASS_JOYSTICK) == InputDevice.SOURCE_CLASS_JOYSTICK;
        int port = pad ? -1 : 0;
        List<InputDevice.MotionRange> axes = device.getMotionRanges();
        List<Integer> fullAxes = new ArrayList<>();
        List<Integer> halfAxes = new ArrayList<>();
        for (InputDevice.MotionRange range : axes) {
            if ((range.getSource() & InputDevice.SOURCE_CLASS_MASK) != InputDevice.SOURCE_CLASS_JOYSTICK)
                // Ignore mouse/touchpad axes
                continue;
            if (range.getMin() == 0)
                halfAxes.add(range.getAxis());
            else
                fullAxes.add(range.getAxis());
        }
        joystickAdded(id, device.getName(), port, device.getDescriptor(),
                ArrayUtils.toPrimitive(fullAxes.toArray(new Integer[0])), ArrayUtils.toPrimitive(halfAxes.toArray(new Integer[0])),
                getVibrator(id) != null);
        knownDevices.add(id);
        if (pad) {
            pads.add(id);
            // Ports saved in emu.cfg are left aside: the pads join on their own
            joystickSetPort(id, port);
        }
        return true;
    }

    private static boolean isBuiltIn(int id) {
        InputDevice device = InputDevice.getDevice(id);
        return device != null && BUILT_IN_CONTROLLER_NAME.equals(device.getName());
    }

    private static int findBuiltIn() {
        for (int id : InputDevice.getDeviceIds())
            if (isBuiltIn(id))
                return id;
        return 0;
    }

    private int portOf(int id) {
        for (int port = 0; port < PORT_COUNT; port++)
            if (portHolders[port] == id)
                return port;
        return -1;
    }

    /**
     * Like a Switch, a pad that plays on no port joins on a button press: on port A if the
     * handheld's own pad or no pad holds it, else on the first free port. The handheld's own
     * pad takes port A back when no one else plays.
     */
    private void claimPort(int id) {
        if (!pads.contains(id) || portOf(id) >= 0)
            return;
        boolean builtIn = isBuiltIn(id);
        boolean othersFree = true;
        for (int port = 1; port < PORT_COUNT; port++)
            othersFree &= portHolders[port] == 0;
        int port = -1;
        if (builtIn && othersFree)
            port = 0;
        else {
            for (int p = 0; p < PORT_COUNT; p++) {
                int holder = portHolders[p];
                if (holder == 0 || (!builtIn && isBuiltIn(holder))) {
                    port = p;
                    break;
                }
            }
        }
        if (port >= 0)
            assignPort(port, id);
    }

    private void assignPort(int port, int id) {
        int previous = portOf(id);
        if (previous >= 0)
            portHolders[previous] = 0;
        int holder = portHolders[port];
        if (holder != 0 && holder != id)
            joystickSetPort(holder, -1);
        portHolders[port] = id;
        joystickSetPort(id, port);
        InputDevice device = InputDevice.getDevice(id);
        Log.i("flycast", (device != null ? device.getName() : String.valueOf(id)) + " plays as player " + (port + 1));
    }

    private static boolean joinsOnPress(int button) {
        return KeyEvent.isGamepadButton(button)
                || (button >= KeyEvent.KEYCODE_DPAD_UP && button <= KeyEvent.KEYCODE_DPAD_RIGHT);
    }

    public boolean buttonEvent(int id, int button, boolean pressed)
    {
        if (!createDevice(id))
            return false;
        if (pressed && joinsOnPress(button))
            claimPort(id);
        return joystickButtonEvent(id, button, pressed);
    }
    public boolean axisEvent(int id, int button, int value)
    {
        if (!createDevice(id))
            return false;
        return joystickAxisEvent(id, button, value);
    }

    public static InputDeviceManager getInstance() {
        return INSTANCE;
    }

    public native void init();
    public native void virtualReleaseAll();
    public native void virtualJoystick(float x, float y);
    public native void virtualButtonInput(int key, boolean pressed);
    private native boolean joystickButtonEvent(int id, int button, boolean pressed);
    private native boolean joystickAxisEvent(int id, int button, int value);
    public native void mouseEvent(int xpos, int ypos, int buttons);
    public native void mouseScrollEvent(int scrollValue);
    public native void touchMouseEvent(int xpos, int ypos, int buttons);
    private native void joystickAdded(int id, String name, int maple_port, String uniqueId, int[] fullAxes, int[] halfAxes, boolean rumbleEnabled);
    private native void joystickRemoved(int id);
    private native void joystickSetPort(int id, int port);
    public native boolean keyboardEvent(int key, boolean pressed);
    public native void keyboardText(int c);
    public static native boolean isMicPluggedIn();
}
