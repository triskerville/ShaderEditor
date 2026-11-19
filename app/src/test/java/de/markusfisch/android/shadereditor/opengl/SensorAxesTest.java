package de.markusfisch.android.shadereditor.opengl;

import static org.junit.Assert.assertArrayEquals;

import org.junit.Test;

public class SensorAxesTest {
	private static final float EPSILON = 1e-5f;

	@Test
	public void vectorsFollowTheScreenAxes() {
		float[] device = {1, 2, 3};
		float[] out = new float[3];
		SensorAxes.vectorToScreen(0, device, out);
		assertArrayEquals(new float[]{1, 2, 3}, out, 0f);
		SensorAxes.vectorToScreen(1, device, out);
		assertArrayEquals(new float[]{-2, 1, 3}, out, 0f);
		SensorAxes.vectorToScreen(2, device, out);
		assertArrayEquals(new float[]{-1, -2, 3}, out, 0f);
		SensorAxes.vectorToScreen(3, device, out);
		assertArrayEquals(new float[]{2, -1, 3}, out, 0f);
	}

	@Test
	public void quaternionMatchesRemappedRotationMatrix() {
		// An arbitrary device orientation.
		float[] q = normalize(new float[]{.3f, -.5f, .2f, .8f});
		float[] deviceToWorld = toMatrix(q);
		for (int rotation = 0; rotation < 4; ++rotation) {
			float[] screenQ = new float[4];
			SensorAxes.quaternionToScreen(rotation, q, screenQ);
			assertArrayEquals(
					remapForDisplay(deviceToWorld, rotation),
					toMatrix(screenQ),
					EPSILON);
		}
	}

	@Test
	public void screenVectorsAreConsistentWithScreenRotation() {
		// R_world_from_device * v_device == R_world_from_screen * v_screen
		float[] q = normalize(new float[]{-.1f, .4f, .6f, .5f});
		float[] v = {.7f, -1.3f, 2.1f};
		float[] world = multiply(toMatrix(q), v);
		for (int rotation = 0; rotation < 4; ++rotation) {
			float[] screenQ = new float[4];
			float[] screenV = new float[3];
			SensorAxes.quaternionToScreen(rotation, q, screenQ);
			SensorAxes.vectorToScreen(rotation, v, screenV);
			assertArrayEquals(world, multiply(toMatrix(screenQ), screenV),
					EPSILON);
		}
	}

	@Test
	public void missingScalarComponentIsReconstructed() {
		float[] q = normalize(new float[]{.3f, -.5f, .2f, .8f});
		float[] withW = new float[4];
		float[] withoutW = new float[4];
		SensorAxes.quaternionToScreen(1, q, withW);
		SensorAxes.quaternionToScreen(1,
				new float[]{q[0], q[1], q[2]}, withoutW);
		assertArrayEquals(withW, withoutW, EPSILON);
	}

	private static float[] normalize(float[] q) {
		float l = (float) Math.sqrt(q[0] * q[0] + q[1] * q[1] +
				q[2] * q[2] + q[3] * q[3]);
		return new float[]{q[0] / l, q[1] / l, q[2] / l, q[3] / l};
	}

	// Same as SensorManager.getRotationMatrixFromVector (row-major).
	private static float[] toMatrix(float[] q) {
		float x = q[0], y = q[1], z = q[2], w = q[3];
		return new float[]{
				1 - 2 * y * y - 2 * z * z, 2 * x * y - 2 * z * w, 2 * x * z + 2 * y * w,
				2 * x * y + 2 * z * w, 1 - 2 * x * x - 2 * z * z, 2 * y * z - 2 * x * w,
				2 * x * z - 2 * y * w, 2 * y * z + 2 * x * w, 1 - 2 * x * x - 2 * y * y,
		};
	}

	// Same as SensorManager.remapCoordinateSystem with the axes that
	// BuiltinSensorUniforms uses for each display rotation.
	private static float[] remapForDisplay(float[] in, int rotation) {
		// {source column for new X, sign, source column for new Y, sign}
		int[][] map = {
				{0, 1, 1, 1},
				{1, -1, 0, 1},
				{0, -1, 1, -1},
				{1, 1, 0, -1},
		};
		int[] m = map[rotation];
		float[] out = new float[9];
		for (int row = 0; row < 3; ++row) {
			out[row * 3] = m[1] * in[row * 3 + m[0]];
			out[row * 3 + 1] = m[3] * in[row * 3 + m[2]];
			out[row * 3 + 2] = in[row * 3 + 2];
		}
		return out;
	}

	private static float[] multiply(float[] m, float[] v) {
		return new float[]{
				m[0] * v[0] + m[1] * v[1] + m[2] * v[2],
				m[3] * v[0] + m[4] * v[1] + m[5] * v[2],
				m[6] * v[0] + m[7] * v[1] + m[8] * v[2],
		};
	}
}
