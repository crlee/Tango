package com.ologicinc.rostango.tango_depthimage;

import android.graphics.Rect;
import android.graphics.YuvImage;

import com.google.common.base.Preconditions;

import org.jboss.netty.buffer.ChannelBufferOutputStream;
import org.ros.internal.message.MessageBuffers;
import org.ros.message.Time;
import org.ros.namespace.NameResolver;
import org.ros.node.ConnectedNode;
import org.ros.node.topic.Publisher;

/**
 * Publishes preview frames.
 *
 * @author damonkohler@google.com (Damon Kohler)
 */
class DepthPublisher implements RawImageListener {

    private final ConnectedNode connectedNode;
    private final Publisher<sensor_msgs.Image> imagePublisher;
    private final Publisher<sensor_msgs.CameraInfo> cameraInfoPublisher;

    private byte[] rawImageBuffer;
    private int rawImageSize;
    private YuvImage yuvImage;
    private Rect rect;
    private ChannelBufferOutputStream stream;

    public DepthPublisher(ConnectedNode connectedNode) {
        this.connectedNode = connectedNode;
        NameResolver resolver = connectedNode.getResolver().newChild("camera/depth");
        imagePublisher =
                connectedNode.newPublisher(resolver.resolve("image_raw"),
                        sensor_msgs.Image._TYPE);
        cameraInfoPublisher =
                connectedNode.newPublisher(resolver.resolve("camera_info"), sensor_msgs.CameraInfo._TYPE);
        stream = new ChannelBufferOutputStream(MessageBuffers.dynamicBuffer());
    }

    @Override
    public void onNewRawImage(int[] data, int width, int height) {
        Preconditions.checkNotNull(data);
        Preconditions.checkNotNull(width);
        Preconditions.checkNotNull(height);

        Time currentTime = connectedNode.getCurrentTime();
        String frameId = "camera";

        sensor_msgs.Image image = imagePublisher.newMessage();
        image.getHeader().setStamp(currentTime);
        image.getHeader().setFrameId(frameId);

        for (int i = 0; i < data.length; i++){
            try {
                stream.writeInt(data[i]);
            }
            catch (Exception e){

            }
        }
        image.setWidth(width);
        image.setHeight(height);
        image.setEncoding("16UC1");
        image.setData(stream.buffer().copy());
        stream.buffer().clear();

        imagePublisher.publish(image);

        sensor_msgs.CameraInfo cameraInfo = cameraInfoPublisher.newMessage();
        cameraInfo.getHeader().setStamp(currentTime);
        cameraInfo.getHeader().setFrameId(frameId);

        cameraInfo.setWidth(width);
        cameraInfo.setHeight(height);
        cameraInfoPublisher.publish(cameraInfo);
    }
}