package dev.lupluv.cb.casino;

import org.bukkit.entity.Player;

import java.util.Random;

public class Coinflip {

    Player p1;
    Player p2;

    long bet1;
    long bet2;

    CoinSide tip1;
    CoinSide tip2;

    CoinSide result;

    public void flip(){
        Random random = new Random();
        int i = random.nextInt(2);
        if(i == 0){
            result = CoinSide.HEAD;
        }else {
            result = CoinSide.NUMBER;
        }
    }

    public Coinflip(Player p1, Player p2) {
        this.p1 = p1;
        this.p2 = p2;
    }

    public Player getP1() {
        return p1;
    }

    public void setP1(Player p1) {
        this.p1 = p1;
    }

    public Player getP2() {
        return p2;
    }

    public void setP2(Player p2) {
        this.p2 = p2;
    }

    public long getBet1() {
        return bet1;
    }

    public void setBet1(long bet1) {
        this.bet1 = bet1;
    }

    public long getBet2() {
        return bet2;
    }

    public void setBet2(long bet2) {
        this.bet2 = bet2;
    }

    public CoinSide getTip1() {
        return tip1;
    }

    public void setTip1(CoinSide tip1) {
        this.tip1 = tip1;
    }

    public CoinSide getTip2() {
        return tip2;
    }

    public void setTip2(CoinSide tip2) {
        this.tip2 = tip2;
    }
}
