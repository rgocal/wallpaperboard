package com.dm.wallpaper.board.services;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import com.danimahardhika.android.helpers.core.FileHelper;
import com.danimahardhika.android.helpers.permission.PermissionHelper;
import com.dm.wallpaper.board.databases.Database;
import com.dm.wallpaper.board.helpers.MuzeiHelper;
import com.dm.wallpaper.board.helpers.WallpaperHelper;
import com.dm.wallpaper.board.items.Wallpaper;
import com.dm.wallpaper.board.preferences.Preferences;
import com.google.android.apps.muzei.api.Artwork;
import com.google.android.apps.muzei.api.RemoteMuzeiArtSource;

import java.io.File;

/*
 * Wallpaper Board
 *
 * Copyright (c) 2017 Dani Mahardhika
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

public abstract class WallpaperBoardMuzeiService extends RemoteMuzeiArtSource {

    private MuzeiHelper mMuzeiHelper;

    public WallpaperBoardMuzeiService(String name) {
        super(name);
    }

    public void startCommand(Intent intent) {
        Bundle bundle = intent.getExtras();
        if (bundle != null) {
            boolean restart = intent.getBooleanExtra("restart", false);
            if (restart) {
                try {
                    onTryUpdate(UPDATE_REASON_USER_NEXT);
                } catch (RetryException ignored) {}
            }
        }
    }

    public void initMuzeiService() {
        super.onCreate();
        mMuzeiHelper = new MuzeiHelper(this);
        setUserCommands(BUILTIN_COMMAND_ID_NEXT_ARTWORK);
    }

    public void tryUpdate(String wallpaperUrl) {
        try {
            if (Preferences.get(this).isConnectedAsPreferred()) {
                Wallpaper wallpaper = mMuzeiHelper.getRandomWallpaper(wallpaperUrl);
                if (wallpaper != null) {
                    publishArtwork(wallpaper);
                }

                Database.get(this).closeDatabase();
            }
        } catch (Exception ignored) {}
    }

    private void publishArtwork(Wallpaper wallpaper) {
        String fileName = wallpaper.getName() +"."+ WallpaperHelper.getFormat(wallpaper.getMimeType());
        File file = new File(WallpaperHelper.getDefaultWallpapersDirectory(this), fileName);

        Uri uri = null;
        if (file.exists() && PermissionHelper.isStorageGranted(this))
            uri = FileHelper.getUriFromFile(this, getPackageName(), file);
        if (uri == null) uri = Uri.parse(wallpaper.getUrl());

        publishArtwork(new Artwork.Builder()
                .title(wallpaper.getName())
                .byline(wallpaper.getAuthor())
                .imageUri(uri)
                .build());

        scheduleUpdate(System.currentTimeMillis() +
                Preferences.get(this).getRotateTime());
    }
}
