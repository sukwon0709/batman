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
-- Encodes data for HTTP transport
--
-- @str data to be encoded
--
function urlencode(str)
   if (str) then
      str = string.gsub (str, "\n", "\r\n")
      str = string.gsub (str, "([^%w ])",
         function (c) return string.format ("%%%02X", string.byte(c)) end)
      str = string.gsub (str, " ", "+")
   end
   return str    
end

---
-- Sends HTTP GET request
--
-- @param addr HTTP url to send GET request to
--
function doGet(addr)
    print("GET url: " .. addr)
    http.request{url = addr}
end

---
-- Sends HTTP POST request
--
-- @param post_data HTTP POST body
--
function doPost(post_data)
    print("POST url: " .. portal_addr .. " data: " .. post_data .. "\n")
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
    local post_content = "[query][" .. sql_index .. "][" .. packet:sub(2) .. "]\n"
    local post_str = post_type .. "&" .. post_index .. "&content=" .. urlencode(post_content)
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
    local row_delimiter = "##"
    local field_delimiter = "$$"
    local assign_delimiter = "^^"
    local packet = inj.query
    local query = packet:sub(2)
    local res = inj.resultset
    local tokens = tokenizer.tokenize(query)
    local token = tokens[1]
    local resp_type = token.token_name
    local post_content = ""
    local vars = ""
    if resp_type == "TK_SQL_SELECT" then
        post_content = "[response][" .. index .. "][select]["
        if res.query_status == proxy.MYSQLD_PACKET_ERR then
            post_content = post_content .. "FAILURE"
        else
            post_content = post_content .. "SUCCESS"
        end
        if res.rows ~= nil then
            local fn = 1
            local fields = res.fields
            while fields[fn] do
                fn = fn + 1
            end
            fn = fn - 1
            for row in res.rows do
                vars = vars .. row_delimiter
                for i = 1, fn do
                    if i > 1 then
                        vars = vars .. field_delimiter
                    end
                    vars = vars .. fields[i].name .. assign_delimiter .. row[i]
                end
                vars = vars .. row_delimiter
            end
        end
        post_content = post_content .. vars .. "]\n"
    else
        local query_status = "STATUS_OK"
        if res.query_status == proxy.MYSQLD_PACKET_ERR then
            query_status = "STATUS_ERR"
        end
        local num_rows = 0
        if res.rows ~= nil then
            num_rows = #res.rows
        end
        post_content = "[response][" .. index .. "][nselect][" .. query_status .. "][" .. tostring(num_rows) .. "]\n"
    end
    local post_str = post_type .. "&" .. post_index .. "&content=" .. urlencode(post_content)
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
    send_response(inj, inj.id)
end
