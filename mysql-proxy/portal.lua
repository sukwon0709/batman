--[[ $%BEGINLICENSE%$
 Copyright (c) 2007, 2012, Oracle and/or its affiliates. All rights reserved.

 This program is free software; you can redistribute it and/or
 modify it under the terms of the GNU General Public License as
 published by the Free Software Foundation; version 2 of the
 License.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 02110-1301  USA

 $%ENDLICENSE%$ --]]
--[[

   

--]]

local http = require("socket.http")
local ltn12 = require("ltn12")
local tokenizer = require("proxy.tokenizer")
local sql_index = 1
local portal_addr = "http://localhost:8080/portal/"

---
-- Sends HTTP GET request
--
-- @param addr HTTP url to send GET request to
--
function doGet(addr)
    http.request{url = addr}
end

---
-- Sends HTTP POST request
--
-- @param post_data HTTP POST body
--
function doPost(post_data)
    http.request(portal_addr, post_data)
end

---
-- Sends GET and POST requests to Portal
--
-- @param packet the mysql-packet sent by the client
--
function send_query( packet )
    local get_str = portal_addr .. "?type=SQL_QUERY&index=" .. sql_index
    doGet(get_str)

    local post_type = "type=SQL_QUERY"
    local post_index = "index=" .. sql_index
    local post_content = "content=[query][" .. sql_index .. "][" .. packet:sub(2) .. "]\n"
    local post_str = post_type .. "&" .. post_index .. "&" .. post_content
    doPost(post_str)
end

---
-- Sends GET and POST requests to Portal
--
-- @param inj
-- @index index for the SQL response
--
function send_response( inj, index )
    local get_str = portal_addr .. "?type=SQL_RESPONSE&index=" .. index
    doGet(get_str)

    local post_type = "type=SQL_RESPONSE"
    local post_index = "index=" .. index
    local packet = inj.query
    local query = packet:sub(2)
    local res = inj.resultset
    local tokens = tokenizer.tokenize(query)
    local token = tokens[1]
    local resp_type = token.token_name
    local post_content = ""
    if resp_type == "TK_SQL_SELECT" then
        post_content = "content=[response][" .. index .. "][select][" .. query .. "]\n"
    else
        local query_status = "STATUS_OK"
        if res.query_status == proxy.MYSQLD_PACKET_ERR then
            query_status = "STATUS_ERR"
        end
        local row_count = 0
        for row in res.rows do
            row_count = row_count + 1
        end
        post_content = "content=[response][" .. index .. "][nselect][" .. query_status .. "][" .. tostring(row_count) .. "]\n"
    end
    local post_str = post_type .. "&" .. post_index .. "&" .. post_content
    doPost(post_str)
end

---
-- read_query() can rewrite packets
--
-- You can use read_query() to replace the packet sent by the client and rewrite
-- query as you like
--
-- @param packet the mysql-packet sent by the client
--
-- @return 
--   * nothing to pass on the packet as is, 
--   * proxy.PROXY_SEND_QUERY to send the queries from the proxy.queries queue
--   * proxy.PROXY_SEND_RESULT to send your own result-set
--
function read_query( packet )
	if string.byte(packet) == proxy.COM_QUERY then
        local tokens = tokenizer.tokenize(packet:sub(2))
        for i = 1, #tokens do
            local token = tokens[i]
            local token_name = token["token_name"]
            if token_name == "TK_SQL_SELECT" or
                token_name == "TK_SQL_INSERT" or
                token_name == "TK_SQL_UPDATE" or
                token_name == "TK_SQL_DELETE" then

		        print("we got a normal query: " .. string.sub(packet, 2))
                send_query(packet)
		        proxy.queries:append(sql_index, packet, { resultset_is_needed = true } )
                sql_index = sql_index + 1

		        return proxy.PROXY_SEND_QUERY
            end
        end
	end
end

---
-- read_query_result() is called when we receive a query result 
-- from the server
--
-- we can analyze the response, drop the response and pass it on (default)
--
-- as we injected a SELECT NOW() above, we don't want to tell the client about it
-- and drop the result with proxy.PROXY_IGNORE_RESULT
-- 
-- @return 
--   * nothing or proxy.PROXY_SEND_RESULT to pass the result-set to the client
--   * proxy.PROXY_IGNORE_RESULT to drop the result-set
-- 
function read_query_result(inj)
	print("injected result-set: id = " .. inj.id)

    send_response(inj, inj.id)
    for row in inj.resultset.rows do
        if row[1] ~= nil then
            print("injected query returned: " .. row[1])
        end
    end
end
