/*
 * Copyright (C) 2015 hu
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package cn.edu.hfut.dmic.webcollector.model;

//import cn.edu.hfut.dmic.webcollector.util.CrawlDatumFormater;
import cn.edu.hfut.dmic.webcollector.util.CrawlDatumFormater;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map.Entry;
import org.apache.hadoop.io.Writable;

/**
 * 爬取任务的数据结构
 * @author hu
 */
public class CrawlDatum implements Writable{

    public final static int STATUS_DB_UNFETCHED = 0;
    public final static int STATUS_DB_INJECT = 1;
    public final static int STATUS_DB_FORCED_INJECT = 2;
    public final static int STATUS_DB_FETCHED = 5;

    private String url = null;
    private long fetchTime = System.currentTimeMillis();

    private int httpCode = -1;
    private int status = STATUS_DB_UNFETCHED;
    private int retry = 0;

    /**
     * 在WebCollector 2.5之后，不再根据URL去重，而是根据key去重
     * 可以通过getKey()方法获得CrawlDatum的key,如果key为null,getKey()方法会返回URL
     * 因此如果不设置key，爬虫会将URL当做key作为去重标准
     */
    private String key = null;

    /**
     * 在WebCollector 2.5之后，可以为每个CrawlDatum添加附加信息metaData
     * 附加信息并不是为了持久化数据，而是为了能够更好地定制爬取任务
     * 在visit方法中，可以通过page.getMetaData()方法来访问CrawlDatum中的metaData
     */
    private HashMap<String, String> metaData = new HashMap<String, String>();

    public CrawlDatum() {
    }

    public CrawlDatum(String url) {
        this.url = url;
    }

    public CrawlDatum(String url, String[] metas) throws Exception {
        this(url);
        if (metas.length % 2 != 0) {
            throw new Exception("length of metas must be even");
        } else {
            for (int i = 0; i < metas.length; i += 2) {
                putMetaData(metas[i * 2], metas[i * 2 + 1]);
            }
        }
    }

    public int incrRetry(int count) {
        retry = retry + count;
        return retry;
    }

    public int getHttpCode() {
        return httpCode;
    }

    public void setHttpCode(int httpCode) {
        this.httpCode = httpCode;
    }

    public String getUrl() {
        return url;
    }

    public CrawlDatum setUrl(String url) {
        this.url = url;
        return this;
    }

    public long getFetchTime() {
        return fetchTime;
    }

    public void setFetchTime(long fetchTime) {
        this.fetchTime = fetchTime;
    }

    public int getRetry() {
        return retry;
    }

    public void setRetry(int retry) {
        this.retry = retry;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public HashMap<String, String> getMetaData() {
        return metaData;
    }

    public void setMetaData(HashMap<String, String> metaData) {
        this.metaData = metaData;
    }

    public CrawlDatum putMetaData(String key, String value) {
        this.metaData.put(key, value);
        return this;
    }

    public String getMetaData(String key) {
        return this.metaData.get(key);
    }

    public String getKey() {
        if (key == null) {
            return getUrl();
        } else {
            return key;
        }
    }

    public CrawlDatum setKey(String key) {
        this.key = key;
        return this;
    }
    
    
    
    @Override
    public String toString(){
        return CrawlDatumFormater.datumToString(this);
    }

    @Override
    public void write(DataOutput d) throws IOException {
        d.writeUTF(getKey());
        d.writeUTF(url);
        d.writeInt(status);
        d.writeLong(fetchTime);
        d.writeInt(httpCode);
        d.writeInt(retry);
        int metaLen=metaData.size();
        d.writeInt(metaLen);
        for(Entry<String,String> entry:metaData.entrySet()){
            d.writeUTF(entry.getKey());
            d.writeUTF(entry.getValue());
        }
    }

    @Override
    public void readFields(DataInput di) throws IOException {
        key=di.readUTF();
        url=di.readUTF();
        status=di.readInt();
        fetchTime=di.readLong();
        httpCode=di.readInt();
        retry=di.readInt();
        int metaLen=di.readInt();
        for(int i=0;i<metaLen;i++){
            String key=di.readUTF();
            String value=di.readUTF();
            metaData.put(key, value);
        }
    }
    
    
    public CrawlDatum copy(){
        CrawlDatum datum=new CrawlDatum(url);
        datum.setKey(key);
        datum.setStatus(status);
        datum.setFetchTime(fetchTime);
        datum.setHttpCode(httpCode);
         for(Entry<String,String> entry:metaData.entrySet()){
             datum.putMetaData(entry.getKey(), entry.getValue());
         }
         return datum;
    }


}
