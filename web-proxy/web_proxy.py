#!/usr/bin/env python

"""
WebProxy.java is reimplemented in Python using mitmproxy.
"""
import errno
import requests
import os
import sys

log_id = 2
req_file_id = 1
resp_file_id = 1

log_dir = "/home/soh/trace/Batman/wackopicko/log_{0}".format(log_id)
html_dir = "{0}/html".format(log_dir)
req_file = None

portal_addr = "http://localhost:8080/portal"

role = 1
user_id = "scanner2"

def start(context, argv):
    """
        Called once on script startup, before any other events.
    """
    global req_file, role, user_id
    try:
        os.makedirs(log_dir)
        os.makedirs(html_dir)
    except OSError as exception:
        if exception.errno != errno.EEXIST:
            raise
    req_file_name = "{0}/{1}-requestFile".format(log_dir, log_id)
    req_file = open(req_file_name, 'a')

    # sends HTTP_SESSION index to Portal
    index = "{0} {1}".format(role, user_id)
    get_req = "{0}?type=HTTP_SESSION&index={1}".format(portal_addr, index)
    r = requests.get(get_req)
    if r.status_code < 200 or r.status_code >= 300:
        print "Sending an index to Portal failed."

def request(context, flow):
    """
        Sends an index to Portal for the incoming request,
        and writes an entry to request file.
    """
    global log_id, req_file_id
    index = "{0}-{1}".format(log_id, req_file_id)
    index_entry = "[HTTP_REQUEST][{0}]".format(index)
    get_req = "{0}?type=HTTP_REQUEST&index={1}".format(portal_addr, index)
    r = requests.get(get_req)
    if r.status_code < 200 or r.status_code >= 300:
        print "Sending an index to Portal failed."

    method = flow.request.method
    url = flow.request.url
    req_entry = "[{0}][{1}][{2}]".format(index, method, url)
    if method == 'POST':
        encoding_format = flow.request.headers['Content-Type']
        if 'application/x-www-form-urlencoded' in encoding_format:
            # ignore file uploads using multipart format
            post_params = flow.request.content.split("&")
            if post_params:
                post_params_str = "&".join(post_params)
                req_entry += "[{0}]".format(post_params_str)
    req_entry += "\n"

    req_file.write(req_entry)
    req_file_id += 1


def response(context, flow):
    """
        Sends an index to Portal for the outgoing reply,
        and writes the HTML page to html file.
    """
    global log_id, resp_file_id
    index = "{0}-{1}".format(log_id, resp_file_id)
    index_entry = "[HTTP_RESPONSE][{0}]".format(index)
    get_req = "{0}?type=HTTP_RESPONSE&index={1}".format(portal_addr, index)
    r = requests.get(get_req)
    if r.status_code < 200 or r.status_code >= 300:
        print "Sending an index to Portal failed."

    html_file_name = "{0}/{1}.html".format(html_dir, resp_file_id)
    with open(html_file_name, "a") as html_file:
        page = flow.response.get_decoded_content()
        html_file.write(page)
        resp_file_id += 1


def done(context):
    """
        Called once on script shutdown, after any other events.
    """
    global req_file
    req_file.close()
