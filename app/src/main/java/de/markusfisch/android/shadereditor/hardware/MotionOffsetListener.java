package de.markusfisch.android.shadereditor.hardware;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;

import androidx.annotation.NonNull;

/**
 * Turns quick movements of the device into a short-lived offset in world
 * space (x = east, y = north, z = up, in metres).
 * <p>
 * The accelerometer can't measure position: integrating it twice drifts
 * away within seconds. So velocity and position are both pulled back to
 * zero continuously. A quick lift moves the offset up, and once the device
 * stops moving, it sinks back to the origin.
 */
public class MotionOffsetListener extends AbstractListener {
	public final float[] offset = new float[]{0, 0, 0};

	private static final float NS2S = 1f / 1000000000f;
	// Skip integration across gaps, e.g. after the screen was off.
	private static final float MAX_DT = .1f;
	// Ignore accelerometer noise below this (m/s^2) so a resting device
	// doesn't wobble.
	private static final float DEAD_ZONE = .15f;
	// How fast velocity fades (1/s). Lower values follow longer
	// movements but drift more.
	private static final float VELOCITY_DECAY = 1.5f;
	// How fast the offset sinks back to the origin (1/s).
	private static final float POSITION_DECAY = 2f;
	// Keep a glitch from flinging the camera far away (m).
	private static final float MAX_OFFSET = 1f;

	// Unit quaternion x, y, z, w that rotates device into world space.
	@NonNull
	private final float[] deviceToWorld;
	private final float[] rotation = new float[9];
	private final float[] velocity = new float[3];

	/**
	 * @param deviceToWorld rotation vector to bring accelerations into
	 *                      world space, updated by someone else
	 */
	public MotionOffsetListener(Context context, @NonNull float[] deviceToWorld) {
		super(context);
		this.deviceToWorld = deviceToWorld;
	}

	public boolean register() {
		offset[0] = offset[1] = offset[2] = 0f;
		velocity[0] = velocity[1] = velocity[2] = 0f;
		return register(Sensor.TYPE_LINEAR_ACCELERATION);
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		if (last > 0) {
			float dt = (event.timestamp - last) * NS2S;
			if (dt > 0 && dt < MAX_DT) {
				integrate(event.values, dt);
			}
		}
		last = event.timestamp;
	}

	private void integrate(@NonNull float[] deviceAcceleration, float dt) {
		float ax = deadZone(deviceAcceleration[0]);
		float ay = deadZone(deviceAcceleration[1]);
		float az = deadZone(deviceAcceleration[2]);

		// world = R * device, R row-major.
		SensorManager.getRotationMatrixFromVector(rotation, deviceToWorld);
		float wx = rotation[0] * ax + rotation[1] * ay + rotation[2] * az;
		float wy = rotation[3] * ax + rotation[4] * ay + rotation[5] * az;
		float wz = rotation[6] * ax + rotation[7] * ay + rotation[8] * az;

		// Exponential decay keeps the feel independent of the sample rate.
		float velocityKeep = (float) Math.exp(-VELOCITY_DECAY * dt);
		float positionKeep = (float) Math.exp(-POSITION_DECAY * dt);
		velocity[0] = (velocity[0] + wx * dt) * velocityKeep;
		velocity[1] = (velocity[1] + wy * dt) * velocityKeep;
		velocity[2] = (velocity[2] + wz * dt) * velocityKeep;
		for (int i = 0; i < 3; ++i) {
			float p = (offset[i] + velocity[i] * dt) * positionKeep;
			offset[i] = Math.max(-MAX_OFFSET, Math.min(MAX_OFFSET, p));
		}
	}

	private static float deadZone(float v) {
		if (v > DEAD_ZONE) {
			return v - DEAD_ZONE;
		} else if (v < -DEAD_ZONE) {
			return v + DEAD_ZONE;
		}
		return 0f;
	}
}
