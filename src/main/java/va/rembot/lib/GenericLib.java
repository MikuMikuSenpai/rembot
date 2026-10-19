package va.rembot.lib;

import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.utils.FileUpload;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class GenericLib {

    public static void sendTextAsAttachedFile(Event event, String text, String fileName, String channelId) {
        File file = new File(fileName);

        try {
            if (file.createNewFile()) {
                FileWriter fw = new FileWriter(file);
                fw.write(text);
                fw.flush();
                fw.close();
                event.getJDA()
                        .getChannelById(TextChannel.class, channelId)
                        .sendFiles(FileUpload.fromData(file))
                        .queue();
                file.delete();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
