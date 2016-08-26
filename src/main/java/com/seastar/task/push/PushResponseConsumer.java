package com.seastar.task.push;

import org.apache.http.HttpResponse;
import org.apache.http.nio.IOControl;
import org.apache.http.nio.client.methods.AsyncCharConsumer;
import org.apache.http.protocol.HttpContext;

import java.io.IOException;
import java.nio.CharBuffer;

/**
 * Created by wjl on 2016/8/25.
 */
public class PushResponseConsumer extends AsyncCharConsumer<String> {
    private StringBuffer sbuf = new StringBuffer();

    @Override
    protected void onResponseReceived(final HttpResponse response) {
    }

    @Override
    protected void onCharReceived(final CharBuffer buf, final IOControl ioctrl) throws IOException {
        sbuf.append(buf);
    }

    @Override
    protected void releaseResources() {
    }

    @Override
    protected String buildResult(final HttpContext context) {
        return sbuf.toString();
    }
}
