package dev.lupluv.cb.broadcast;

import dev.lupluv.cb.Citybuild;
import dev.lupluv.cb.economy.Economy;
import dev.lupluv.cb.scorebaord.ScoreboardManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;

import java.util.List;

public class BroadcastMessages {

    private List<String> messages;
    private int currentMessage;

    private int taskID;

    public void loadBroadCastMessage(){

        taskID = Bukkit.getScheduler().scheduleSyncRepeatingTask(Citybuild.getPlugin(), () ->{

            Bukkit.getOnlinePlayers().forEach(player ->{
                player.sendMessage(
                        ScoreboardManager.format2(messages.get(currentMessage)
                                .replace("%player%", player.getName())
                                .replace("%money%", String.valueOf(Economy.getBalance(player.getUniqueId())))
                        )
                );
            });

            if(currentMessage == messages.size()-1){
                currentMessage = 0;
            }else{
                currentMessage++;
            }

        }, 0, 20*60*2+20*45);

    }

    public BroadcastMessages(List<String> messages, int currentMessage) {
        this.messages = messages;
        this.currentMessage = currentMessage;
        loadBroadCastMessage();
    }

    public int getTaskID() {
        return taskID;
    }

    public void setTaskID(int taskID) {
        this.taskID = taskID;
    }

    public List<String> getMessages() {
        return messages;
    }

    public void setMessages(List<String> messages) {
        this.messages = messages;
    }

    public int getCurrentMessage() {
        return currentMessage;
    }

    public void setCurrentMessage(int currentMessage) {
        this.currentMessage = currentMessage;
    }
}
