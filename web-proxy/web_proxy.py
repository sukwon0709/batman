#!/usr/bin/env python

"""
WebProxy.java is reimplemented in Python using mitmproxy.
"""
import errno
import os
import sys

log_id = 1
req_file_id = 1
resp_file_id = 1

log_dir = "."
html_dir = "{0}/html".format(log_dir)
req_file = None

def start(context, argv):
    """
        Called once on script startup, before any other events.
    """
    global req_file
    req_file_name = "{0}/{1}-requestFile".format(log_dir, log_id)
    req_file = open(req_file_name, 'w')
    try:
        os.makedirs(html_dir)
    except OSError as exception:
        if exception.errno != errno.EEXIST:
            raise

def request(context, flow):
    """
        Sends an index to Portal for the incoming request,
        and writes an entry to request file.
    """
    global log_id, req_file_id
    index = "{0}-{1}".format(log_id, req_file_id)
    index_entry = "[HTTP_REQUEST][{0}]".format(index)

    method = flow.request.method
    url = flow.request.url
    req_entry = "[{0}][{1}][{2}]".format(index, method, url)
    if method == 'POST':
        post_params = flow.request.content.split("&")
        for post_param in post_params:
            param_name = post_param.split("=")[0]
            req_entry += "[{0}]".format(param_name)
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

    html_file_name = "{0}/{1}.html".format(html_dir, resp_file_id)
    with open(html_file_name, "w") as html_file:
        page = flow.response.get_decoded_content()
        html_file.write(page)
        resp_file_id += 1


def done(context):
    """
        Called once on script shutdown, after any other events.
    """
    global req_file
    req_file.close()
