/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.documentsui.inspector;

import android.content.Context;
import android.content.res.Resources;
import android.media.ExifInterface;
import android.media.MediaMetadata;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.support.annotation.VisibleForTesting;
import android.text.format.DateUtils;
import android.util.AttributeSet;

import com.android.documentsui.R;
import com.android.documentsui.base.DocumentInfo;
import com.android.documentsui.base.Shared;
import com.android.documentsui.inspector.InspectorController.MediaDisplay;
import com.android.documentsui.inspector.InspectorController.TableDisplay;

import javax.annotation.Nullable;

/**
 * Organizes and Displays the debug information about a file. This view
 * should only be made visible when build is debuggable and system policies
 * allow debug "stuff".
 */
public class MediaView extends TableView implements MediaDisplay {

    private final Resources mResources;

    public MediaView(Context context) {
        this(context, null);
    }

    public MediaView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MediaView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mResources = context.getResources();
    }

    @Override
    public void accept(DocumentInfo doc, Bundle metadata, @Nullable Runnable geoClickListener) {
        setTitle(R.string.inspector_metadata_section);

        Bundle exif = metadata.getBundle(DocumentsContract.METADATA_EXIF);
        if (exif != null) {
            showExifData(this, mResources, doc, exif, geoClickListener);
        }

        Bundle video = metadata.getBundle(Shared.METADATA_KEY_VIDEO);
        if (video != null) {
            showVideoData(this, mResources, doc, video, geoClickListener);
        }

        setVisible(!isEmpty());
    }

    @VisibleForTesting
    public static void showVideoData(
            TableDisplay table,
            Resources resources,
            DocumentInfo doc,
            Bundle tags,
            @Nullable Runnable geoClickListener) {

        addDimensionsRow(table, resources, tags);

        if (MetadataUtils.hasVideoCoordinates(tags)) {
            float[] coords = MetadataUtils.getVideoCoords(tags);
            showCoordiantes(table, resources, coords, geoClickListener);
        }

        if (tags.containsKey(MediaMetadata.METADATA_KEY_DURATION)) {
            int millis = tags.getInt(MediaMetadata.METADATA_KEY_DURATION);
            table.put(R.string.metadata_duration, DateUtils.formatElapsedTime(millis / 1000));
        }
    }

    @VisibleForTesting
    public static void showExifData(
            TableDisplay table,
            Resources resources,
            DocumentInfo doc,
            Bundle tags,
            @Nullable Runnable geoClickListener) {

        addDimensionsRow(table, resources, tags);

        if (tags.containsKey(ExifInterface.TAG_DATETIME)) {
            String date = tags.getString(ExifInterface.TAG_DATETIME);
            table.put(R.string.metadata_date_time, date);
        }

        if (MetadataUtils.hasExifGpsFields(tags)) {
            float[] coords = MetadataUtils.getExifGpsCoords(tags);
            showCoordiantes(table, resources, coords, geoClickListener);
        }

        if (tags.containsKey(ExifInterface.TAG_GPS_ALTITUDE)) {
            double altitude = tags.getDouble(ExifInterface.TAG_GPS_ALTITUDE);
            table.put(R.string.metadata_altitude, String.valueOf(altitude));
        }

        if (tags.containsKey(ExifInterface.TAG_MAKE) || tags.containsKey(ExifInterface.TAG_MODEL)) {
                String make = tags.getString(ExifInterface.TAG_MAKE);
                String model = tags.getString(ExifInterface.TAG_MODEL);
                make = make != null ? make : "";
                model = model != null ? model : "";
                table.put(
                        R.string.metadata_camera,
                        resources.getString(R.string.metadata_camera_format, make, model));
        }

        if (tags.containsKey(ExifInterface.TAG_APERTURE)) {
            table.put(R.string.metadata_aperture, resources.getString(
                    R.string.metadata_aperture_format, tags.getDouble(ExifInterface.TAG_APERTURE)));
        }

        if (tags.containsKey(ExifInterface.TAG_SHUTTER_SPEED_VALUE)) {
            String shutterSpeed = String.valueOf(
                    formatShutterSpeed(tags.getDouble(ExifInterface.TAG_SHUTTER_SPEED_VALUE)));
            table.put(R.string.metadata_shutter_speed, shutterSpeed);
        }

        if (tags.containsKey(ExifInterface.TAG_FOCAL_LENGTH)) {
            double length = tags.getDouble(ExifInterface.TAG_FOCAL_LENGTH);
            table.put(R.string.metadata_focal_length,
                    String.format(resources.getString(R.string.metadata_focal_format), length));
        }

        if (tags.containsKey(ExifInterface.TAG_ISO_SPEED_RATINGS)) {
            int iso = tags.getInt(ExifInterface.TAG_ISO_SPEED_RATINGS);
            table.put(R.string.metadata_iso_speed_ratings,
                    String.format(resources.getString(R.string.metadata_iso_format), iso));
        }

    }

    private static void showCoordiantes(
            TableDisplay table,
            Resources resources,
            float[] coords,
            @Nullable Runnable geoClickListener) {

        String value = resources.getString(
                R.string.metadata_coordinates_format, coords[0], coords[1]);
        if (geoClickListener != null) {
            table.put(
                    R.string.metadata_coordinates,
                    value,
                    view -> {
                        geoClickListener.run();
                    }
            );
        } else {
            table.put(R.string.metadata_coordinates, value);
        }
    }

    /**
     * @param speed a value n, where shutter speed equals 1/(2^n)
     * @return a String containing either a fraction that displays 1 over a positive integer, or a
     * double rounded to one decimal, depending on if 1/(2^n) is less than or greater than 1,
     * respectively.
     */
    private static String formatShutterSpeed(double speed) {
        if (speed <= 0) {
            double shutterSpeed = Math.pow(2, -1 * speed);
            String formattedSpeed = String.valueOf(Math.round(shutterSpeed * 10.0) / 10.0);
            return formattedSpeed;
        } else {
            int approximateSpeedDenom = (int) Math.pow(2, speed) + 1;
            String formattedSpeed = "1/" + String.valueOf(approximateSpeedDenom);
            return formattedSpeed;
        }
    }

    /**
     * @param table
     * @param resources
     * @param tags
     */
    private static void addDimensionsRow(TableDisplay table, Resources resources, Bundle tags) {
        if (tags.containsKey(ExifInterface.TAG_IMAGE_WIDTH)
            && tags.containsKey(ExifInterface.TAG_IMAGE_LENGTH)) {
            int width = tags.getInt(ExifInterface.TAG_IMAGE_WIDTH);
            int height = tags.getInt(ExifInterface.TAG_IMAGE_LENGTH);
            float megaPixels = height * width / 1000000f;
            table.put(R.string.metadata_dimensions,
                    resources.getString(
                            R.string.metadata_dimensions_format, width, height, megaPixels));
        }
    }
}