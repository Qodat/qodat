/*
 * Copyright (C) 2015 Square, Inc.
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
package stan.gifencoder;

import stan.gifencoder.ColorQuantizer;
import stan.gifencoder.DisposalMethod;
import stan.gifencoder.FloydSteinbergDitherer;
import stan.gifencoder.MedianCutQuantizer;

import java.util.concurrent.TimeUnit;

public final class ImageOptions {
    int left = 0;
    int top = 0;
    ColorQuantizer quantizer = MedianCutQuantizer.INSTANCE;
    Ditherer ditherer = FloydSteinbergDitherer.INSTANCE;
    DisposalMethod disposalMethod = DisposalMethod.UNSPECIFIED;
    int delayCentiSeconds = 0;
    int transColor = 0;
    boolean tranSet = false;
    boolean usePreviousColors = false;


    /**
     * Create a new {@link ImageOptions} with all the defaults.
     */
    public ImageOptions() {
    }

    public ImageOptions setUsePreviousColors(boolean usePrevious) {
        this.usePreviousColors = usePrevious;
        return this;
    }

    public ImageOptions setLeft(int left) {
        this.left = left;
        return this;
    }

    public ImageOptions setTop(int top) {
        this.top = top;
        return this;
    }

    public ImageOptions setColorQuantizer(ColorQuantizer quantizer) {
        this.quantizer = quantizer;
        return this;
    }

    public ImageOptions setDitherer(Ditherer ditherer) {
        this.ditherer = ditherer;
        return this;
    }

    public ImageOptions setDisposalMethod(DisposalMethod disposalMethod) {
        this.disposalMethod = disposalMethod;
        return this;
    }

    public ImageOptions setDelay(long duration, TimeUnit unit) {
        this.delayCentiSeconds = (int) (unit.toMillis(duration) / 10);
        return this;
    }

    public ImageOptions setTransparencyColor(int transparency) {
        this.transColor = transparency;
        this.tranSet = true;
        return this;
    }
}

