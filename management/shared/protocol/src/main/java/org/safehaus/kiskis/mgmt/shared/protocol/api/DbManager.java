/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.safehaus.kiskis.mgmt.shared.protocol.api;

import com.datastax.driver.core.ResultSet;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;

/**
 *
 * @author dilshat
 */
public interface DbManager {

    public ResultSet executeQuery(String cql, Object... values);

    public void executeUpdate(String cql, Object... values);

    public void saveInfo(String source, String key, Serializable info) throws IOException;

    public <T> List<T> getInfo(String source, String key, Class<T> clazz) throws ClassNotFoundException, IOException;

    public <T> List<T> getInfo(String source, Class<T> clazz) throws ClassNotFoundException, IOException;
}
