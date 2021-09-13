package org.vitrivr.cineast.core.mms.Helper;

import it.sauronsoftware.jave.AudioAttributes;
import it.sauronsoftware.jave.Encoder;
import it.sauronsoftware.jave.EncoderException;
import it.sauronsoftware.jave.EncodingAttributes;

import java.io.File;

public class AudioExtractor {

    public static void convertToMP3(String input, String output) { //modify on your convenience
        File source = new File(input);
        File target = new File(output);

        AudioAttributes audioAttributes = new AudioAttributes();
        audioAttributes.setCodec("libmp3lame");
        audioAttributes.setBitRate(new Integer(128000));
        audioAttributes.setChannels(new Integer(2));
        audioAttributes.setSamplingRate(new Integer(44100));
        EncodingAttributes encodingAttributes = new EncodingAttributes();
        encodingAttributes.setFormat("mp3");
        encodingAttributes.setAudioAttributes(audioAttributes);

        Encoder encoder = new Encoder();
        try {
            encoder.encode(source, target, encodingAttributes);
        } catch (EncoderException e) {
            e.printStackTrace();
        }
    }
    public static void main(String args[])
    {

        convertToMP3("C:\\DEV\\cineast\\cineast-core\\src\\main\\java\\org\\vitrivr\\cineast\\core\\mms\\Data\\audioext.mp4", "C:\\DEV\\cineast\\cineast-core\\src\\main\\java\\org\\vitrivr\\cineast\\core\\mms\\Data\\audioext-output.mp3");
    }
}
