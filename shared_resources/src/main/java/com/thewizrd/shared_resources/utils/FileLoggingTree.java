package com.thewizrd.shared_resources.utils;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.thewizrd.shared_resources.AsyncTask;

import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.ZoneOffset;
import org.threeten.bp.format.DateTimeFormatter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import timber.log.Timber;

public class FileLoggingTree extends Timber.Tree {

    private static final String TAG = FileLoggingTree.class.getSimpleName();
    private static boolean ranCleanup = false;

    private Context context;

    public FileLoggingTree(Context context) {
        this.context = context;
    }

    @Override
    protected void log(int priority, String tag, @NonNull String message, Throwable t) {

        try {

            final File directory = new File(context.getExternalFilesDir(null) + "/logs");

            if (!directory.exists()) {
                directory.mkdir();
            }

            final LocalDateTime today = LocalDateTime.now(ZoneOffset.UTC);

            final String dateTimeStamp = today.format(DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ROOT));
            String logTimeStamp = today.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss:SSS", Locale.ROOT));

            final String logNameFormat = "Logger.%s.log";
            String fileName = String.format(Locale.ROOT, logNameFormat, dateTimeStamp);

            File file = new File(directory.getPath() + File.separator + fileName);

            if (!file.exists())
                file.createNewFile();

            if (file.exists()) {

                OutputStream fileOutputStream = new FileOutputStream(file, true);

                String priorityTAG;
                switch (priority) {
                    default:
                    case Log.DEBUG:
                        priorityTAG = "DEBUG";
                        break;
                    case Log.INFO:
                        priorityTAG = "INFO";
                        break;
                    case Log.VERBOSE:
                        priorityTAG = "VERBOSE";
                        break;
                    case Log.WARN:
                        priorityTAG = "WARN";
                        break;
                    case Log.ERROR:
                        priorityTAG = "ERROR";
                        break;
                }

                if (t != null)
                    fileOutputStream.write((logTimeStamp + "|" + priorityTAG + "|" + (tag == null ? "" : tag + "|") + message + "\n" + t.toString() + "\n").getBytes());
                else
                    fileOutputStream.write((logTimeStamp + "|" + priorityTAG + "|" + (tag == null ? "" : tag + "|") + message + "\n").getBytes());

                fileOutputStream.close();
            }

            // Cleanup old logs if they exist
            if (!ranCleanup) {
                AsyncTask.run(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            // Only keep a weeks worth of logs
                            int daysToKeep = 7;

                            // Get todays date
                            LocalDate todayLocal = today.toLocalDate();

                            // Create a list of the last 7 day's dates
                            final List<String> dateStampsToKeep = new ArrayList<>();
                            for (int i = 0; i < daysToKeep; i++) {
                                LocalDate date = todayLocal.minusDays(i);
                                String dateStamp = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ROOT));

                                dateStampsToKeep.add(String.format(Locale.ROOT, logNameFormat, dateStamp));
                            }

                            // List all log files not in the above list
                            File[] logs = directory.listFiles(new FilenameFilter() {
                                @Override
                                public boolean accept(File dir, String name) {
                                    return name.startsWith("Logger") && !dateStampsToKeep.contains(name);
                                }
                            });

                            // Delete all log files in the array above
                            for (File logToDel : logs) {
                                logToDel.delete();
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error cleaning up log files : " + e);
                        }
                    }
                });
                ranCleanup = true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error while logging into file : " + e);
        }
    }
}
