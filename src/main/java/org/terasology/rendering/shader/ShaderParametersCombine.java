/*
 * Copyright 2013 Moving Blocks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terasology.rendering.shader;

import org.lwjgl.opengl.GL13;
import org.terasology.config.Config;
import org.terasology.engine.CoreRegistry;
import org.terasology.rendering.assets.material.Material;
import org.terasology.rendering.cameras.Camera;
import org.terasology.rendering.opengl.DefaultRenderingProcess;
import org.terasology.rendering.world.WorldRenderer;

import javax.vecmath.Vector3f;
import javax.vecmath.Vector4f;

/**
 * Shader parameters for the Combine shader program.
 *
 * @author Benjamin Glatzel <benjamin.glatzel@me.com>
 */
public class ShaderParametersCombine extends ShaderParametersBase {

    private float outlineDepthThreshold = 0.001f;
    private float outlineThickness = 0.65f;

    private float skyInscatteringLength = 0.25f;
    private float skyInscatteringStrength = 0.35f;
    private float skyInscatteringThreshold = 0.75f;

    private float volFogDensityAtViewer = 0.15f;
    private float volFogGlobalDensity = 0.05f;
    private float volFogHeightFalloff = 0.1f;

    @Override
    public void applyParameters(Material program) {
        super.applyParameters(program);

        int texId = 0;

        DefaultRenderingProcess.FBO sceneOpaque = DefaultRenderingProcess.getInstance().getFBO("sceneOpaque");

        if (sceneOpaque != null) {
            GL13.glActiveTexture(GL13.GL_TEXTURE0 + texId);
            sceneOpaque.bindTexture();
            program.setInt("texSceneOpaque", texId++, true);

            GL13.glActiveTexture(GL13.GL_TEXTURE0 + texId);
            sceneOpaque.bindDepthTexture();
            program.setInt("texSceneOpaqueDepth", texId++, true);

            GL13.glActiveTexture(GL13.GL_TEXTURE0 + texId);
            sceneOpaque.bindNormalsTexture();
            program.setInt("texSceneOpaqueNormals", texId++, true);

            GL13.glActiveTexture(GL13.GL_TEXTURE0 + texId);
            sceneOpaque.bindLightBufferTexture();
            program.setInt("texSceneOpaqueLightBuffer", texId++, true);
        }

        if (CoreRegistry.get(Config.class).getRendering().isVolumetricFog()) {
            Camera activeCamera = CoreRegistry.get(WorldRenderer.class).getActiveCamera();
            if (activeCamera != null) {
                program.setMatrix4("invViewProjMatrix", activeCamera.getInverseViewProjectionMatrix(), true);

                Vector3f fogWorldPosition = new Vector3f(activeCamera.getPosition().x, 32.0f, activeCamera.getPosition().y);
                fogWorldPosition.sub(activeCamera.getPosition());
                program.setFloat3("fogWorldPosition", fogWorldPosition.x, fogWorldPosition.y, fogWorldPosition.z, true);
            }

            program.setFloat4("volumetricFogSettings", volFogDensityAtViewer,
                    volFogGlobalDensity, volFogHeightFalloff, 0.0f, true);
        }

        DefaultRenderingProcess.FBO sceneTransparent = DefaultRenderingProcess.getInstance().getFBO("sceneTransparent");

        if (sceneTransparent != null) {
            GL13.glActiveTexture(GL13.GL_TEXTURE0 + texId);
            sceneTransparent.bindTexture();
            program.setInt("texSceneTransparent", texId++, true);
        }

        if (CoreRegistry.get(Config.class).getRendering().isSsao()) {
            GL13.glActiveTexture(GL13.GL_TEXTURE0 + texId);
            DefaultRenderingProcess.getInstance().bindFboTexture("ssaoBlurred");
            program.setInt("texSsao", texId++, true);
        }

        if (CoreRegistry.get(Config.class).getRendering().isOutline()) {
            GL13.glActiveTexture(GL13.GL_TEXTURE0 + texId);
            DefaultRenderingProcess.getInstance().bindFboTexture("sobel");
            program.setInt("texEdges", texId++, true);

            program.setFloat("outlineDepthThreshold", outlineDepthThreshold, true);
            program.setFloat("outlineThickness", outlineThickness, true);
        }

        GL13.glActiveTexture(GL13.GL_TEXTURE0 + texId);
        DefaultRenderingProcess.getInstance().bindFboTexture("sceneSkyBand1");
        program.setInt("texSceneSkyBand", texId++, true);

        Vector4f skyInscatteringSettingsFrag = new Vector4f();
        skyInscatteringSettingsFrag.y = skyInscatteringStrength;
        skyInscatteringSettingsFrag.z = skyInscatteringLength;
        skyInscatteringSettingsFrag.w = skyInscatteringThreshold;
        program.setFloat4("skyInscatteringSettingsFrag", skyInscatteringSettingsFrag, true);
    }
}