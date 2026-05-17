package com.iptv.player;

public class Channel {
    private String name;
    private String url;
    private String logo;
    private String epg;
    private int number;

    public Channel() {}

    public Channel(String name, String url, String logo, String epg, int number) {
        this.name = name;
        this.url = url;
        this.logo = logo;
        this.epg = epg;
        this.number = number;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getLogo() { return logo; }
    public void setLogo(String logo) { this.logo = logo; }
    public String getEpg() { return epg; }
    public void setEpg(String epg) { this.epg = epg; }
    public int getNumber() { return number; }
    public void setNumber(int number) { this.number = number; }
}
