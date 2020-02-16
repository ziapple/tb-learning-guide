package com.ziapple.transport.service;

import com.ziapple.transport.TransportProtos;
import com.ziapple.transport.api.SessionMsgListener;
import com.ziapple.transport.api.TransportService;
import com.ziapple.transport.api.TransportServiceCallback;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class LocalTransportServiceMock implements TransportService {

    @Override
    public void process(TransportProtos.ValidateDeviceTokenRequestMsg msg, TransportServiceCallback<TransportProtos.ValidateDeviceCredentialsResponseMsg> callback) {

    }

    @Override
    public void process(TransportProtos.ValidateDeviceX509CertRequestMsg msg, TransportServiceCallback<TransportProtos.ValidateDeviceCredentialsResponseMsg> callback) {

    }

    @Override
    public void process(TransportProtos.GetOrCreateDeviceFromGatewayRequestMsg msg, TransportServiceCallback<TransportProtos.GetOrCreateDeviceFromGatewayResponseMsg> callback) {

    }

    @Override
    public boolean checkLimits(TransportProtos.SessionInfoProto sessionInfo, Object msg, TransportServiceCallback<Void> callback) {
        return false;
    }

    @Override
    public void process(TransportProtos.SessionInfoProto sessionInfo, TransportProtos.SessionEventMsg msg, TransportServiceCallback<Void> callback) {

    }

    @Override
    public void process(TransportProtos.SessionInfoProto sessionInfo, TransportProtos.PostTelemetryMsg msg, TransportServiceCallback<Void> callback) {

    }

    @Override
    public void process(TransportProtos.SessionInfoProto sessionInfo, TransportProtos.PostAttributeMsg msg, TransportServiceCallback<Void> callback) {

    }

    @Override
    public void process(TransportProtos.SessionInfoProto sessionInfo, TransportProtos.GetAttributeRequestMsg msg, TransportServiceCallback<Void> callback) {

    }

    @Override
    public void process(TransportProtos.SessionInfoProto sessionInfo, TransportProtos.SubscribeToAttributeUpdatesMsg msg, TransportServiceCallback<Void> callback) {

    }

    @Override
    public void process(TransportProtos.SessionInfoProto sessionInfo, TransportProtos.SubscribeToRPCMsg msg, TransportServiceCallback<Void> callback) {

    }

    @Override
    public void process(TransportProtos.SessionInfoProto sessionInfo, TransportProtos.ToDeviceRpcResponseMsg msg, TransportServiceCallback<Void> callback) {

    }

    @Override
    public void process(TransportProtos.SessionInfoProto sessionInfo, TransportProtos.ToServerRpcRequestMsg msg, TransportServiceCallback<Void> callback) {

    }

    @Override
    public void process(TransportProtos.SessionInfoProto sessionInfo, TransportProtos.SubscriptionInfoProto msg, TransportServiceCallback<Void> callback) {

    }

    @Override
    public void process(TransportProtos.SessionInfoProto sessionInfo, TransportProtos.ClaimDeviceMsg msg, TransportServiceCallback<Void> callback) {

    }

    @Override
    public void registerAsyncSession(TransportProtos.SessionInfoProto sessionInfo, SessionMsgListener listener) {

    }

    @Override
    public void registerSyncSession(TransportProtos.SessionInfoProto sessionInfo, SessionMsgListener listener, long timeout) {

    }

    @Override
    public void reportActivity(TransportProtos.SessionInfoProto sessionInfo) {

    }

    @Override
    public void deregisterSession(TransportProtos.SessionInfoProto sessionInfo) {

    }
}
