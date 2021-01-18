package de.eliaspr;

import net.dv8tion.jda.api.entities.TextChannel;

import java.util.Calendar;

public class Countdown implements Runnable {

    static {

    }

    private final Lecture lecture;
    private final TextChannel channel;
    private boolean shouldStop = false;
    private Runnable onCountdownEnd = () -> {
    };

    public Countdown(Lecture lecture, TextChannel channel) {
        this.lecture = lecture;
        this.channel = channel;
    }

    public synchronized void setOnCountdownEnd(Runnable onCountdownEnd) {
        this.onCountdownEnd = onCountdownEnd;
    }

    public synchronized void stop() {
        shouldStop = true;
    }

    public Thread createThread() {
        Thread th = new Thread(this);
        th.setDaemon(true);
        return th;
    }

    @Override
    public void run() {
        String oldTopic = channel.getTopic();
        channel.sendMessage("Countdown aktiv :thumbsup:").queue();
        long nextUpdateTime = 0;
        int lastFinalCountdownBroadcast = -1;

        while (true) {
            Calendar c = Calendar.getInstance();
            int dayAgeMinutes = c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE);
            int remainingSeconds = (lecture.endTime - dayAgeMinutes) * 60 - c.get(Calendar.SECOND);
            if (remainingSeconds <= 0) {
                channel.getManager().setTopic(oldTopic).queue();
                channel.sendMessage(lecture.name + " ist zu Ende! :beer:").queue();
                break;
            }

            if (remainingSeconds > 5 * 60) {
                int remainingMinutes = remainingSeconds / 60;
                long now = System.currentTimeMillis();
                if (now > nextUpdateTime) {
                    int remainingHours = remainingMinutes / 60;
                    remainingMinutes %= 60;
                    String topic = String.format("%s ist in %02d:%02d zu Ende", lecture.name, remainingHours, remainingMinutes);
                    System.out.println(topic);
                    channel.getManager().setTopic(topic).queue();
                    nextUpdateTime = now + 5 * 60 * 1000;
                }
            } else if (remainingSeconds <= 10) {
                if (lastFinalCountdownBroadcast != remainingSeconds) {
                    lastFinalCountdownBroadcast = remainingSeconds;
                    channel.sendMessage(String.valueOf(remainingSeconds)).queue();
                }
            }

            synchronized (this) {
                if (shouldStop)
                    break;
            }

            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                break;
            }
        }

        channel.getManager().setTopic(oldTopic).queue();
        synchronized (this) {
            onCountdownEnd.run();
        }
    }
}
