package pervacio.com.testhandlerthread;

import android.content.Context;
import android.util.SparseArray;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import pervacio.com.testhandlerthread.callbacks.LifeCycleCallback;
import pervacio.com.testhandlerthread.callbacks.TaskCallbacks;
import pervacio.com.testhandlerthread.tasks.DownloadTask;
import pervacio.com.testhandlerthread.tasks.UploadTask;
import pervacio.com.testhandlerthread.utils.CommonUtils;
import pervacio.com.testhandlerthread.utils.Constants;
import pervacio.com.testhandlerthread.utils.FutureWaiter;

import static pervacio.com.testhandlerthread.utils.Constants.CHARSET;
import static pervacio.com.testhandlerthread.utils.Constants.DOWNLOAD;
import static pervacio.com.testhandlerthread.utils.Constants.DOWNLOAD_URL;
import static pervacio.com.testhandlerthread.utils.Constants.UPLOAD;
import static pervacio.com.testhandlerthread.utils.Constants.UPLOAD_URL;

public class TestRouter {

//    private static int counter;
//    private final AtomicBoolean mCancelled = new AtomicBoolean();

    private ExecutorService mExecutor;
    private Context mContext;

    @Constants.NetworkType
    private int mNetworkType;
    private SparseArray<TaskCallbacks> mCallbackMap;

//    private TaskCallbacks taskCallbacks;

    private TestRouter(@Constants.NetworkType int networkType, SparseArray<TaskCallbacks> callbackMap, Context context) {
        mNetworkType = networkType;
        mCallbackMap = callbackMap;
        mContext = context;
        mExecutor = Executors.newSingleThreadExecutor();
    }

    public void start(long maxDuration) {
        executeAndClear(maxDuration);
    }

    public void addTaskAndStart(@Constants.MeasureTaskType int type, TaskCallbacks callbacks, long maxDuration) {
        mCallbackMap.append(type, callbacks);
        executeAndClear(maxDuration);
    }

    public void addTask(@Constants.MeasureTaskType int type, TaskCallbacks callbacks) {
        mCallbackMap.append(type, callbacks);
    }

    private void executeAndClear(long maxDuration) {
        if (mCallbackMap.size() == 0) {
            throw new RuntimeException("No actions to execute");
        }
        String connectionErrorMessage = CommonUtils.getConnectionErrorMessage(mNetworkType, mContext);
        final LifeCycleCallback first = mCallbackMap.get(mCallbackMap.keyAt(0));
        final LifeCycleCallback last = mCallbackMap.get(mCallbackMap.size() - 1);
        Future<Float> lastTaskFuture = null;

        first.onStartRouting();
        for (int i = 0; i < mCallbackMap.size(); i++) {
            int key = mCallbackMap.keyAt(i);
            switch (key) {
                case DOWNLOAD:
                    lastTaskFuture = mExecutor.submit(new DownloadTask(DOWNLOAD_URL, maxDuration, mCallbackMap.get(key)).getCallable());
                    break;
                case UPLOAD:
                    lastTaskFuture = mExecutor.submit(new UploadTask(UPLOAD_URL, CHARSET, maxDuration, mCallbackMap.get(key)).getCallable());
                    break;
            }
        }
        waitForLastTaskCompleted(maxDuration, last, lastTaskFuture);
        mCallbackMap.clear();
    }

    private void waitForLastTaskCompleted(long maxDuration, LifeCycleCallback last, Future<Float> lastTaskFuture) {
        new FutureWaiter(lastTaskFuture, maxDuration * mCallbackMap.size() , last);
    }

    public void finishRouting() {
        if (mExecutor != null) {
            mExecutor.shutdown();
            mExecutor = null;
        }
    }

   public static class Builder {

        private SparseArray<TaskCallbacks> mCallbackMap;
        @Constants.NetworkType
        private int mNetworkType;
        private Context mContext;

        public Builder(Context context) {
            mContext = context;
            mCallbackMap = new SparseArray<>(2);
            mNetworkType = Constants.WIFI;
        }

        public Builder setNetworkType(@Constants.NetworkType int networkType) {
            mNetworkType = networkType;
            return this;
        }

        public Builder setDownload(TaskCallbacks download) {
            mCallbackMap.append(DOWNLOAD, download);
            return this;
        }

        public Builder setUpload(TaskCallbacks upload) {
            mCallbackMap.append(UPLOAD, upload);
            return this;
        }

        public TestRouter create() {
            return new TestRouter(mNetworkType, mCallbackMap, mContext);
        }

    }


}
