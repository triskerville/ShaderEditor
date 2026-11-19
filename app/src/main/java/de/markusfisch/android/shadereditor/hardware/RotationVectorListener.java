package de.markusfisch.android.shadereditor.hardware;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;

public class RotationVectorListener extends AbstractListener {
	// Unit quaternion x, y, z, w.
	public final float[] values = new float[]{0, 0, 0, 1};

	public RotationVectorListener(Context context) {
		super(context);
	}

	public boolean register() {
		// Prefer TYPE_GAME_ROTATION_VECTOR if possible because it doesn't
		// depend from a geomagnetic sensor.
		return register(Sensor.TYPE_GAME_ROTATION_VECTOR) ||
				register(Sensor.TYPE_ROTATION_VECTOR);
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		float x = event.values[0];
		float y = event.values[1];
		float z = event.values[2];
		values[0] = x;
		values[1] = y;
		values[2] = z;
		// The scalar component is optional on old devices.
		values[3] = event.values.length > 3
				? event.values[3]
				: (float) Math.sqrt(Math.max(0f, 1f - x * x - y * y - z * z));
	}
}
