package de.markusfisch.android.shadereditor.opengl;

import android.view.Surface;

import androidx.annotation.NonNull;

/**
 * Converts sensor data from the device's natural coordinate system into
 * the coordinate system of the screen as it is currently rotated.
 * <p>
 * Android always reports sensor values relative to the device's natural
 * orientation, but shaders work in screen space. Without this conversion,
 * the X and Y axes swap (and flip) when the display is rotated.
 */
final class SensorAxes {
	private SensorAxes() {
	}

	/**
	 * Map a vector (or axial vector like angular velocity) from device
	 * coordinates into screen coordinates. Matches
	 * {@link android.hardware.SensorManager#remapCoordinateSystem} with the
	 * axes recommended for each display rotation.
	 */
	static void vectorToScreen(
			int displayRotation,
			@NonNull float[] in,
			@NonNull float[] out) {
		float x = in[0];
		float y = in[1];
		switch (displayRotation) {
			case Surface.ROTATION_90 -> {
				out[0] = -y;
				out[1] = x;
			}
			case Surface.ROTATION_180 -> {
				out[0] = -x;
				out[1] = -y;
			}
			case Surface.ROTATION_270 -> {
				out[0] = y;
				out[1] = -x;
			}
			default -> {
				out[0] = x;
				out[1] = y;
			}
		}
		out[2] = in[2];
	}

	/**
	 * Map a rotation vector (unit quaternion x, y, z[, w] that rotates
	 * device coordinates into world coordinates) so that it rotates screen
	 * coordinates into world coordinates instead.
	 * <p>
	 * q' = q * r, with r being the rotation of the screen around the
	 * device's Z axis.
	 */
	static void quaternionToScreen(
			int displayRotation,
			@NonNull float[] in,
			@NonNull float[] out) {
		float x = in[0];
		float y = in[1];
		float z = in[2];
		float w = in.length > 3
				? in[3]
				: (float) Math.sqrt(Math.max(0f, 1f - x * x - y * y - z * z));
		double half = -(displayRotation & 3) * Math.PI / 4.0;
		float s = (float) Math.sin(half);
		float c = (float) Math.cos(half);
		out[0] = c * x + s * y;
		out[1] = c * y - s * x;
		out[2] = c * z + s * w;
		if (out.length > 3) {
			out[3] = c * w - s * z;
		}
	}
}
