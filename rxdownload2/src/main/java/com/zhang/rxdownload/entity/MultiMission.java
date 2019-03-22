package com.zhang.rxdownload.entity;

import com.zhang.rxdownload.RxDownload;
import com.zhang.rxdownload.db.DataBaseHelper;
import com.zhang.rxdownload.function.Constant;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.processors.FlowableProcessor;

import static com.zhang.rxdownload.function.DownloadEventFactory.completed;
import static com.zhang.rxdownload.function.DownloadEventFactory.failed;
import static com.zhang.rxdownload.function.DownloadEventFactory.normal;
import static com.zhang.rxdownload.function.DownloadEventFactory.paused;
import static com.zhang.rxdownload.function.DownloadEventFactory.started;
import static com.zhang.rxdownload.function.DownloadEventFactory.waiting;
import static com.zhang.rxdownload.function.Utils.createProcessor;
import static com.zhang.rxdownload.function.Utils.formatStr;


/**
 * Author: Season(ssseasonnn@gmail.com)
 * Date: 2017/2/24
 * <p>
 * MultiMission, many urls.
 */
public class MultiMission extends DownloadMission {
    private AtomicInteger completeNumber;
    private AtomicInteger failedNumber;
    private List<SingleMission> missions;

    private String missionId;

    private Observer<DownloadStatus> observer = new Observer<DownloadStatus>() {
        @Override
        public void onSubscribe(Disposable d) {
            processor.onNext(started(null));
        }


        @Override
        public void onNext(DownloadStatus value) {

        }

        @Override
        public void onError(Throwable e) {
            int temp = failedNumber.incrementAndGet();
            if ((temp + completeNumber.intValue()) == missions.size()) {
                processor.onNext(failed(null, new Throwable("download failedNumber")));
            }
        }

        @Override
        public void onComplete() {
            int temp = completeNumber.incrementAndGet();
            if (temp == missions.size()) {
                processor.onNext(completed(null));
                setCompleted(true);
            } else if ((temp + failedNumber.intValue()) == missions.size()) {
                processor.onNext(failed(null, new Throwable("download failedNumber")));
            }
        }
    };

    public MultiMission(MultiMission other) {
        super(other.rxdownload);
        this.missionId = other.getUrl();
        this.missions = new ArrayList<>();
        this.completeNumber = new AtomicInteger(0);
        this.failedNumber = new AtomicInteger(0);

        for (SingleMission each : other.getMissions()) {
            this.missions.add(new SingleMission(each));
        }
    }

    public MultiMission(RxDownload rxDownload, String missionId, List<DownloadBean> missions) {
        super(rxDownload);
        this.missionId = missionId;
        this.missions = new ArrayList<>();
        this.completeNumber = new AtomicInteger(0);
        this.failedNumber = new AtomicInteger(0);

        for (DownloadBean each : missions) {
            this.missions.add(new SingleMission(rxDownload, each, missionId, observer));
        }
    }

    private List<SingleMission> getMissions() {
        return missions;
    }

    public String getUrl() {
        return missionId;
    }

    @Override
    public void init(Map<String, DownloadMission> missionMap,
                     Map<String, FlowableProcessor<DownloadEvent>> processorMap) {
        DownloadMission mission = missionMap.get(getUrl());
        if (mission == null) {
            missionMap.put(getUrl(), this);
        } else {
            if (mission.isCanceled()) {
                missionMap.put(getUrl(), this);
            } else {
                throw new IllegalArgumentException(formatStr(Constant.DOWNLOAD_URL_EXISTS, getUrl()));
            }
        }

        this.processor = createProcessor(getUrl(), processorMap);

        for (SingleMission each : missions) {
            each.init(missionMap, processorMap);
        }
    }

    @Override
    public void insertOrUpdate(DataBaseHelper dataBaseHelper) {
        for (SingleMission each : missions) {
            each.insertOrUpdate(dataBaseHelper);
        }
    }

    @Override
    public void sendWaitingEvent(DataBaseHelper dataBaseHelper) {
        for (SingleMission each : missions) {
            each.sendWaitingEvent(dataBaseHelper);
        }
        processor.onNext(waiting(null));
    }

    @Override
    public void start(Semaphore semaphore) throws InterruptedException {
        for (SingleMission each : missions) {
            each.start(semaphore);
        }
    }

    @Override
    public void pause(DataBaseHelper dataBaseHelper) {
        for (SingleMission each : missions) {
            each.pause(dataBaseHelper);
        }
        setCanceled(true);
        processor.onNext(paused(null));
    }

    @Override
    public void delete(DataBaseHelper dataBaseHelper, boolean deleteFile) {
        for (SingleMission each : missions) {
            each.delete(dataBaseHelper, deleteFile);
        }
        setCanceled(true);
        processor.onNext(normal(null));
    }
}