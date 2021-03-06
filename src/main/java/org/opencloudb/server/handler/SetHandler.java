/*
 * Copyright (c) 2013, OpenCloudDB/MyCAT and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software;Designed and Developed mainly by many Chinese 
 * opensource volunteers. you can redistribute it and/or modify it under the 
 * terms of the GNU General Public License version 2 only, as published by the
 * Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 * 
 * Any questions about this component can be directed to it's project Web address 
 * https://code.google.com/p/opencloudb/.
 *
 */
package org.opencloudb.server.handler;

import static org.opencloudb.server.parser.ServerParseSet.AUTOCOMMIT_OFF;
import static org.opencloudb.server.parser.ServerParseSet.AUTOCOMMIT_ON;
import static org.opencloudb.server.parser.ServerParseSet.CHARACTER_SET_CLIENT;
import static org.opencloudb.server.parser.ServerParseSet.CHARACTER_SET_CONNECTION;
import static org.opencloudb.server.parser.ServerParseSet.CHARACTER_SET_RESULTS;
import static org.opencloudb.server.parser.ServerParseSet.NAMES;
import static org.opencloudb.server.parser.ServerParseSet.TX_READ_COMMITTED;
import static org.opencloudb.server.parser.ServerParseSet.TX_READ_UNCOMMITTED;
import static org.opencloudb.server.parser.ServerParseSet.TX_REPEATED_READ;
import static org.opencloudb.server.parser.ServerParseSet.TX_SERIALIZABLE;

import org.apache.log4j.Logger;
import org.opencloudb.config.ErrorCode;
import org.opencloudb.config.Isolations;
import org.opencloudb.net.mysql.OkPacket;
import org.opencloudb.server.ServerConnection;
import org.opencloudb.server.parser.ServerParseSet;
import org.opencloudb.server.response.CharacterSet;

/**
 * SET 语句处理
 * 
 * @author mycat
 */
public final class SetHandler {

    private static final Logger logger = Logger.getLogger(SetHandler.class);
    private static final byte[] AC_OFF = new byte[] { 7, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0 };

    public static void handle(String stmt, ServerConnection c, int offset) {
    	//System.out.println("SetHandler: "+stmt);
        int rs = ServerParseSet.parse(stmt, offset);
        switch (rs & 0xff) {
        case AUTOCOMMIT_ON:
            if (c.isAutocommit()) {
                c.write(c.writeToBuffer(OkPacket.OK, c.allocate()));
            } else {
                c.commit();
                c.setAutocommit(true);
            }
            break;
        case AUTOCOMMIT_OFF: {
            if (c.isAutocommit()) {
                c.setAutocommit(false);
            }
            c.write(c.writeToBuffer(AC_OFF, c.allocate()));
            break;
        }
        case TX_READ_UNCOMMITTED: {
            c.setTxIsolation(Isolations.READ_UNCOMMITTED);
            c.write(c.writeToBuffer(OkPacket.OK, c.allocate()));
            break;
        }
        case TX_READ_COMMITTED: {
            c.setTxIsolation(Isolations.READ_COMMITTED);
            c.write(c.writeToBuffer(OkPacket.OK, c.allocate()));
            break;
        }
        case TX_REPEATED_READ: {
            c.setTxIsolation(Isolations.REPEATED_READ);
            c.write(c.writeToBuffer(OkPacket.OK, c.allocate()));
            break;
        }
        case TX_SERIALIZABLE: {
            c.setTxIsolation(Isolations.SERIALIZABLE);
            c.write(c.writeToBuffer(OkPacket.OK, c.allocate()));
            break;
        }
        case NAMES:
            String charset = stmt.substring(rs >>> 8).trim();
            if (c.setCharset(charset)) {
                c.write(c.writeToBuffer(OkPacket.OK, c.allocate()));
            } else {
                c.writeErrMessage(ErrorCode.ER_UNKNOWN_CHARACTER_SET, "Unknown charset '" + charset + "'");
            }
            break;
        case CHARACTER_SET_CLIENT:
        case CHARACTER_SET_CONNECTION:
        case CHARACTER_SET_RESULTS:
            CharacterSet.response(stmt, c, rs);
            break;
        default:
            StringBuilder s = new StringBuilder();
            logger.warn(s.append(c).append(stmt).append(" is not recoginized and ignored").toString());
            c.write(c.writeToBuffer(OkPacket.OK, c.allocate()));
        }
    }

}