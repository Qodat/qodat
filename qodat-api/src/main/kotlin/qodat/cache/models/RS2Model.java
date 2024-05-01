/*
 * Copyright (c) 2017, Adam <Adam@sigterm.info>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package qodat.cache.models;

import qodat.cache.definition.ModelDefinition;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * This is the same as the RuneLite implementation but with vertexGroups being accessible.
 */
public class RS2Model implements ModelDefinition
{
	private String id;

	private int format = 0;
	private int vertexCount = 0;
	private int[] vertexPositionsX;
	private int[] vertexPositionsY;
	private int[] vertexPositionsZ;
	public transient VertexNormal[] vertexNormals;

	private int faceCount;
	private int[] faceVertexIndices1;
	private int[] faceVertexIndices2;
	private int[] faceVertexIndices3;
	private byte[] faceAlphas;
	private short[] faceColors;
	private byte[] faceRenderPriorities;
	private byte[] faceRenderTypes;
	public transient FaceNormal[] faceNormals;

	private int textureConfigCount;
	private short[] textureTriangleVertexIndices1;
	private short[] textureTriangleVertexIndices2;
	private short[] textureTriangleVertexIndices3;
	private transient float[][] faceTextureUCoordinates;
	private transient float[][] faceTextureVCoordinates;
	private short[] texturePrimaryColors;
	private short[] faceTextures;
	private byte[] faceTextureConfigs;
	private byte[] textureRenderTypes;

	private int[] vertexSkins;
	private int[] faceSkins;

	private byte priority;

	private short[] aShortArray2574;
	private short[] aShortArray2575;
	private short[] aShortArray2577;
	private short[] aShortArray2578;
	private byte[] aByteArray2580;
	private short[] aShortArray2586;

	private transient int[][] vertexGroups;
	private transient int[][] faceGroups;

	private int[][] animayaGroups;
	private int[][] animayaScales;


	/**
	 * Computes the UV coordinates for every three-vertex face that has a
	 * texture.
	 */
	@Override
	public void computeTextureUVCoordinates()
	{
		this.faceTextureUCoordinates = new float[faceCount][];
		this.faceTextureVCoordinates = new float[faceCount][];

		for (int i = 0; i < faceCount; i++)
		{
			int textureConfig;
			if (faceTextureConfigs == null)
			{
				textureConfig = -1;
			}
			else
			{
				textureConfig = faceTextureConfigs[i];
			}

			int textureIdx;
			if (faceTextures == null)
			{
				textureIdx = -1;
			}
			else
			{
				textureIdx = faceTextures[i] & 0xFFFF;
			}

			if (textureIdx != -1)
			{
				float[] u = new float[3];
				float[] v = new float[3];

				if (textureConfig == -1)
				{
					u[0] = 0.0F;
					v[0] = 1.0F;

					u[1] = 1.0F;
					v[1] = 1.0F;

					u[2] = 0.0F;
					v[2] = 0.0F;
				}
				else
				{
					textureConfig &= 0xFF;

					byte textureRenderType = 0;
					if (textureRenderTypes != null)
					{
						textureRenderType = textureRenderTypes[textureConfig];
					}

					if (textureRenderType == 0)
					{
						int faceVertexIdx1 = faceVertexIndices1[i];
						int faceVertexIdx2 = faceVertexIndices2[i];
						int faceVertexIdx3 = faceVertexIndices3[i];

						short triangleVertexIdx1 = textureTriangleVertexIndices1[textureConfig];
						short triangleVertexIdx2 = textureTriangleVertexIndices2[textureConfig];
						short triangleVertexIdx3 = textureTriangleVertexIndices3[textureConfig];

						mapToUV(u, v, faceVertexIdx1, faceVertexIdx2, faceVertexIdx3, triangleVertexIdx1, triangleVertexIdx2, triangleVertexIdx3);
					}
				}

				this.faceTextureUCoordinates[i] = u;
				this.faceTextureVCoordinates[i] = v;
			}
		}
	}

	public void mapToUV(float[] u, float[] v, int faceVertexIdx1, int faceVertexIdx2, int faceVertexIdx3, int triangleVertexIdx1, int triangleVertexIdx2, int triangleVertexIdx3) {
		float triangleX = vertexPositionsX[triangleVertexIdx1];
		float triangleY = vertexPositionsY[triangleVertexIdx1];
		float triangleZ = vertexPositionsZ[triangleVertexIdx1];

		float f_882_ = vertexPositionsX[triangleVertexIdx2] - triangleX;
		float f_883_ = vertexPositionsY[triangleVertexIdx2] - triangleY;
		float f_884_ = vertexPositionsZ[triangleVertexIdx2] - triangleZ;
		float f_885_ = vertexPositionsX[triangleVertexIdx3] - triangleX;
		float f_886_ = vertexPositionsY[triangleVertexIdx3] - triangleY;
		float f_887_ = vertexPositionsZ[triangleVertexIdx3] - triangleZ;
		float f_888_ = vertexPositionsX[faceVertexIdx1] - triangleX;
		float f_889_ = vertexPositionsY[faceVertexIdx1] - triangleY;
		float f_890_ = vertexPositionsZ[faceVertexIdx1] - triangleZ;
		float f_891_ = vertexPositionsX[faceVertexIdx2] - triangleX;
		float f_892_ = vertexPositionsY[faceVertexIdx2] - triangleY;
		float f_893_ = vertexPositionsZ[faceVertexIdx2] - triangleZ;
		float f_894_ = vertexPositionsX[faceVertexIdx3] - triangleX;
		float f_895_ = vertexPositionsY[faceVertexIdx3] - triangleY;
		float f_896_ = vertexPositionsZ[faceVertexIdx3] - triangleZ;

		float f_897_ = f_883_ * f_887_ - f_884_ * f_886_;
		float f_898_ = f_884_ * f_885_ - f_882_ * f_887_;
		float f_899_ = f_882_ * f_886_ - f_883_ * f_885_;
		float f_900_ = f_886_ * f_899_ - f_887_ * f_898_;
		float f_901_ = f_887_ * f_897_ - f_885_ * f_899_;
		float f_902_ = f_885_ * f_898_ - f_886_ * f_897_;
		float f_903_ = 1.0F / (f_900_ * f_882_ + f_901_ * f_883_ + f_902_ * f_884_);

		u[0] = (f_900_ * f_888_ + f_901_ * f_889_ + f_902_ * f_890_) * f_903_;
		u[1] = (f_900_ * f_891_ + f_901_ * f_892_ + f_902_ * f_893_) * f_903_;
		u[2] = (f_900_ * f_894_ + f_901_ * f_895_ + f_902_ * f_896_) * f_903_;

		f_900_ = f_883_ * f_899_ - f_884_ * f_898_;
		f_901_ = f_884_ * f_897_ - f_882_ * f_899_;
		f_902_ = f_882_ * f_898_ - f_883_ * f_897_;
		f_903_ = 1.0F / (f_900_ * f_885_ + f_901_ * f_886_ + f_902_ * f_887_);

		v[0] = (f_900_ * f_888_ + f_901_ * f_889_ + f_902_ * f_890_) * f_903_;
		v[1] = (f_900_ * f_891_ + f_901_ * f_892_ + f_902_ * f_893_) * f_903_;
		v[2] = (f_900_ * f_894_ + f_901_ * f_895_ + f_902_ * f_896_) * f_903_;
	}

	@Override
	public void computeAnimationTables()
	{
		if (this.vertexSkins != null)
		{
			int[] groupCounts = new int[256];
			int numGroups = 0;
			int var3, var4;

			for (var3 = 0; var3 < this.vertexCount; ++var3)
			{
				var4 = this.vertexSkins[var3];
				++groupCounts[var4];
				if (var4 > numGroups)
				{
					numGroups = var4;
				}
			}

			this.vertexGroups = new int[numGroups + 1][];

			for (var3 = 0; var3 <= numGroups; ++var3)
			{
				this.vertexGroups[var3] = new int[groupCounts[var3]];
				groupCounts[var3] = 0;
			}

			for (var3 = 0; var3 < this.vertexCount; this.vertexGroups[var4][groupCounts[var4]++] = var3++)
			{
				var4 = this.vertexSkins[var3];
			}
		}
		if (this.faceSkins != null)
		{
			int[] groupCounts = new int[256];
			int numGroups = 0;
			int var3, var4;

			for (var3 = 0; var3 < this.faceCount; ++var3)
			{
				var4 = this.faceSkins[var3];
				++groupCounts[var4];
				if (var4 > numGroups)
				{
					numGroups = var4;
				}
			}

			this.faceGroups = new int[numGroups + 1][];

			for (var3 = 0; var3 <= numGroups; ++var3)
			{
				this.faceGroups[var3] = new int[groupCounts[var3]];
				groupCounts[var3] = 0;
			}

			for (var3 = 0; var3 < this.faceCount; this.faceGroups[var4][groupCounts[var4]++] = var3++)
			{
				var4 = this.faceSkins[var3];
			}
		}
	}
	public void computeNormals()
	{
		if (this.vertexNormals != null)
		{
			return;
		}

		this.vertexNormals = new VertexNormal[this.vertexCount];

		int var1;
		for (var1 = 0; var1 < this.vertexCount; ++var1)
		{
			this.vertexNormals[var1] = new VertexNormal();
		}

		for (var1 = 0; var1 < this.faceCount; ++var1)
		{
			int vertexA = this.faceVertexIndices1[var1];
			int vertexB = this.faceVertexIndices2[var1];
			int vertexC = this.faceVertexIndices3[var1];

			int xA = this.vertexPositionsX[vertexB] - this.vertexPositionsX[vertexA];
			int yA = this.vertexPositionsY[vertexB] - this.vertexPositionsY[vertexA];
			int zA = this.vertexPositionsZ[vertexB] - this.vertexPositionsZ[vertexA];

			int xB = this.vertexPositionsX[vertexC] - this.vertexPositionsX[vertexA];
			int yB = this.vertexPositionsY[vertexC] - this.vertexPositionsY[vertexA];
			int zB = this.vertexPositionsZ[vertexC] - this.vertexPositionsZ[vertexA];

			// Compute cross product
			int var11 = yA * zB - yB * zA;
			int var12 = zA * xB - zB * xA;
			int var13 = xA * yB - xB * yA;

			while (var11 > 8192 || var12 > 8192 || var13 > 8192 || var11 < -8192 || var12 < -8192 || var13 < -8192)
			{
				var11 >>= 1;
				var12 >>= 1;
				var13 >>= 1;
			}

			int length = (int) Math.sqrt((double) (var11 * var11 + var12 * var12 + var13 * var13));
			if (length <= 0)
			{
				length = 1;
			}

			var11 = var11 * 256 / length;
			var12 = var12 * 256 / length;
			var13 = var13 * 256 / length;

			byte var15;
			if (this.faceRenderTypes == null)
			{
				var15 = 0;
			}
			else
			{
				var15 = this.faceRenderTypes[var1];
			}

			if (var15 == 0)
			{
				VertexNormal var16 = this.vertexNormals[vertexA];
				var16.x += var11;
				var16.y += var12;
				var16.z += var13;
				++var16.magnitude;

				var16 = this.vertexNormals[vertexB];
				var16.x += var11;
				var16.y += var12;
				var16.z += var13;
				++var16.magnitude;

				var16 = this.vertexNormals[vertexC];
				var16.x += var11;
				var16.y += var12;
				var16.z += var13;
				++var16.magnitude;
			}
			else if (var15 == 1)
			{
				if (this.faceNormals == null)
				{
					this.faceNormals = new FaceNormal[this.faceCount];
				}

				FaceNormal var17 = this.faceNormals[var1] = new FaceNormal();
				var17.x = var11;
				var17.y = var12;
				var17.z = var13;
			}
		}
	}
	public void setId(String id) {
		this.id = id;
	}

	public void setFormat(int format) {
		this.format = format;
	}

	@Override
	public int getVertexCount() {
		return vertexCount;
	}

	@NotNull
	@Override
	public int[] getVertexPositionsX() {
		return vertexPositionsX;
	}

	@NotNull
	@Override
	public int[] getVertexPositionsY() {
		return vertexPositionsY;
	}

	@NotNull
	@Override
	public int[] getVertexPositionsZ() {
		return vertexPositionsZ;
	}

	@Nullable
	@Override
	public int[] getVertexSkins() {
		return vertexSkins;
	}

	@Nullable
	@Override
	public int[][] getVertexGroups() { return vertexGroups; }

	@Override
	public int getFaceCount() {
		return faceCount;
	}

	@NotNull
	@Override
	public int[] getFaceVertexIndices1() {
		return faceVertexIndices1;
	}

	@NotNull
	@Override
	public int[] getFaceVertexIndices2() {
		return faceVertexIndices2;
	}

	@NotNull
	@Override
	public int[] getFaceVertexIndices3() {
		return faceVertexIndices3;
	}

	@Nullable
	@Override
	public int[] getFaceSkins() {
		return faceSkins;
	}

	@Nullable
	@Override
	public int[][] getFaceGroups() {
		return faceGroups;
	}

	@NotNull
	@Override
	public short[] getFaceColors() {
		return faceColors;
	}

	@Nullable
	@Override
	public byte[] getFaceAlphas() {
		return faceAlphas;
	}

	@Nullable
	@Override
	public byte[] getFacePriorities() {
		return faceRenderPriorities;
	}

	@Nullable
	@Override
	public byte[] getFaceTypes() {
		return faceRenderTypes;
	}

	@Override
	public int getTextureConfigCount() { return textureConfigCount; }

	@Nullable
	@Override
	public byte[] getTextureRenderTypes() {
		return textureRenderTypes;
	}

	@Nullable
	@Override
	public short[] getTextureTriangleVertexIndices1() {
		return textureTriangleVertexIndices1;
	}

	@Nullable
	@Override
	public short[] getTextureTriangleVertexIndices2() {
		return textureTriangleVertexIndices2;
	}

	@Nullable
	@Override
	public short[] getTextureTriangleVertexIndices3() {
		return textureTriangleVertexIndices3;
	}

	@Nullable
	@Override
	public short[] getFaceTextures() {
		return faceTextures;
	}

	@Nullable
	@Override
	public byte[] getFaceTextureConfigs() {
		return faceTextureConfigs;
	}

	@Nullable
	@Override
	public float[][] getFaceTextureUCoordinates() {
		return faceTextureUCoordinates;
	}

	@Nullable
	@Override
	public float[][] getFaceTextureVCoordinates() {
		return faceTextureUCoordinates;
	}

	public byte[] getFaceRenderPriorities() {
		return faceRenderPriorities;
	}

	public byte[] getFaceRenderTypes() {
		return faceRenderTypes;
	}

	public short[] getTexturePrimaryColors() {
		return texturePrimaryColors;
	}

	public void setVertexCount(int vertexCount) {
		this.vertexCount = vertexCount;
	}

	public void setVertexPositionsX(int[] vertexPositionsX) {
		this.vertexPositionsX = vertexPositionsX;
	}

	public void setVertexPositionsY(int[] vertexPositionsY) {
		this.vertexPositionsY = vertexPositionsY;
	}

	public void setVertexPositionsZ(int[] vertexPositionsZ) {
		this.vertexPositionsZ = vertexPositionsZ;
	}

	public void setVertexSkins(int[] vertexSkins) {
		this.vertexSkins = vertexSkins;
	}

	public void setFaceCount(int faceCount) {
		this.faceCount = faceCount;
	}

	public void setFaceVertexIndices1(int[] faceVertexIndices1) {
		this.faceVertexIndices1 = faceVertexIndices1;
	}

	public void setFaceVertexIndices2(int[] faceVertexIndices2) {
		this.faceVertexIndices2 = faceVertexIndices2;
	}

	public void setFaceVertexIndices3(int[] faceVertexIndices3) {
		this.faceVertexIndices3 = faceVertexIndices3;
	}

	public void setFaceSkins(int[] faceSkins) {
		this.faceSkins = faceSkins;
	}

	public void setFaceAlphas(byte[] faceAlphas) {
		this.faceAlphas = faceAlphas;
	}

	public void setFaceColors(short[] faceColors) {
		this.faceColors = faceColors;
	}

	public void setFaceRenderPriorities(byte[] faceRenderPriorities) {
		this.faceRenderPriorities = faceRenderPriorities;
	}

	public void setFaceRenderTypes(byte[] faceRenderTypes) {
		this.faceRenderTypes = faceRenderTypes;
	}

	public void setTexturePrimaryColors(short[] texturePrimaryColors) {
		this.texturePrimaryColors = texturePrimaryColors;
	}

	public void setAnimayaGroups(int[][] animayaGroups) {
		this.animayaGroups = animayaGroups;
	}

	public void setAnimayaScales(int[][] animayaScales) {
		this.animayaScales = animayaScales;
	}

	public int[][] getAnimayaGroups() {
		return animayaGroups;
	}

	public int[][] getAnimayaScales() {
		return animayaScales;
	}

	public void setPriority(byte priority) {
		this.priority = priority;
	}

	@Override
	public byte getPriority() {
		return priority;
	}

	@Override
	public String toString() {
		return "id=" + id +
				"\nformat=" + format +
				"\nvertexCount=" + vertexCount +
				"\nfaceCount=" + faceCount +
				"\ntextureConfigCount=" + textureConfigCount +
				"\npriority=" + priority;
	}

	@NotNull
	@Override
	public String getName() {
		return id;
	}

	@NotNull
	@Override
	public VertexNormal[] getVertexNormals() {
		return vertexNormals;
	}

	@NotNull
	@Override
	public FaceNormal[] getFaceNormals() {
		return faceNormals;
	}


	public void setFaceTextures(short[] faceTextures) {
		this.faceTextures = faceTextures;
	}

	public void setFaceTextureConfigs(byte[] faceTextureConfigs) {
		this.faceTextureConfigs = faceTextureConfigs;
	}

	public void setTextureRenderTypes(byte[] textureRenderTypes) {
		this.textureRenderTypes = textureRenderTypes;
	}

	public void setTextureTriangleVertexIndices1(short[] textureTriangleVertexIndices1) {
		this.textureTriangleVertexIndices1 = textureTriangleVertexIndices1;
	}

	public void setTextureTriangleVertexIndices2(short[] textureTriangleVertexIndices2) {
		this.textureTriangleVertexIndices2 = textureTriangleVertexIndices2;
	}

	public void setTextureTriangleVertexIndices3(short[] textureTriangleVertexIndices3) {
		this.textureTriangleVertexIndices3 = textureTriangleVertexIndices3;
	}

}
