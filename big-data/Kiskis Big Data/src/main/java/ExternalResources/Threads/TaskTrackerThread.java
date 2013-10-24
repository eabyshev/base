/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */
package ExternalResources.Threads;

import org.apache.hadoop.mapred.TaskTracker;

/**
 * Created with IntelliJ IDEA.
 * User: frkn
 * Date: 10/1/13
 * Time: 10:24 AM
 * To change this template use File | Settings | File Templates.
 */
public class TaskTrackerThread extends Thread implements Runnable {
    public void run()
    {
        String [] temp = new String[0];
        try {
            System.out.println("Starting TaskTracker!");
            TaskTracker.main(temp);
        } catch (Exception e) {
            System.out.println("Could not start TaskTracker!");
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }
}
