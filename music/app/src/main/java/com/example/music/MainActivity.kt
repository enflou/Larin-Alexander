package com.example.music

import android.content.Intent
import android.content.SharedPreferences
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.URL

class MainActivity : AppCompatActivity() {

    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var btnSelectFile: Button
    private lateinit var btnLoadUrl: Button
    private lateinit var btnLoadFromUrl: Button
    private lateinit var btnPlay: Button
    private lateinit var btnPause: Button
    private lateinit var btnStop: Button
    private lateinit var btnHistory: Button
    private lateinit var tvTrackInfo: TextView
    private lateinit var tvStatus: TextView
    private lateinit var seekBar: SeekBar
    private lateinit var progressBar: ProgressBar
    private lateinit var urlInputLayout: LinearLayout
    private lateinit var etUrl: EditText

    private val handler = Handler(Looper.getMainLooper())
    private var updateSeekBar: Runnable? = null
    private var currentAudioUri: Uri? = null
    private var currentAudioFile: File? = null
    private var isUrlMode = false
    private var isSeekBarTracking = false

    private lateinit var sharedPreferences: SharedPreferences
    private val trackHistory = mutableListOf<Track>()

    data class Track(
        val name: String,
        val uri: String,
        val type: String, // "file" или "url"
        val timestamp: Long = System.currentTimeMillis()
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setupSharedPreferences()
        loadTrackHistory()
        setupMediaPlayer()
        setupClickListeners()
    }

    private fun initViews() {
        btnSelectFile = findViewById(R.id.btnSelectFile)
        btnLoadUrl = findViewById(R.id.btnLoadUrl)
        btnLoadFromUrl = findViewById(R.id.btnLoadFromUrl)
        btnPlay = findViewById(R.id.btnPlay)
        btnPause = findViewById(R.id.btnPause)
        btnStop = findViewById(R.id.btnStop)
        btnHistory = findViewById(R.id.btnHistory)
        tvTrackInfo = findViewById(R.id.tvTrackInfo)
        tvStatus = findViewById(R.id.tvStatus)
        seekBar = findViewById(R.id.seekBar)
        progressBar = findViewById(R.id.progressBar)
        urlInputLayout = findViewById(R.id.urlInputLayout)
        etUrl = findViewById(R.id.etUrl)
    }

    private fun setupSharedPreferences() {
        sharedPreferences = getSharedPreferences("music_player", MODE_PRIVATE)
    }

    private fun setupMediaPlayer() {
        mediaPlayer = MediaPlayer()

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    tvStatus.text = "Перемотка: ${formatTime(progress)} / ${formatTime(mediaPlayer.duration)}"
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                isSeekBarTracking = true
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                isSeekBarTracking = false
                if (mediaPlayer.isPlaying || currentAudioUri != null || currentAudioFile != null) {
                    val progress = seekBar?.progress ?: 0
                    mediaPlayer.seekTo(progress)
                    tvStatus.text = if (mediaPlayer.isPlaying) "Воспроизведение" else "На паузе"
                }
            }
        })

        updateSeekBar = object : Runnable {
            override fun run() {
                if (mediaPlayer.isPlaying && !isSeekBarTracking) {
                    val currentPosition = mediaPlayer.currentPosition
                    val duration = mediaPlayer.duration
                    seekBar.progress = currentPosition
                    seekBar.max = duration

                    tvStatus.text = "Воспроизведение: ${formatTime(currentPosition)} / ${formatTime(duration)}"
                }
                handler.postDelayed(this, 1000)
            }
        }

        mediaPlayer.setOnCompletionListener {
            runOnUiThread {
                tvStatus.text = "Статус: Воспроизведение завершено"
                seekBar.progress = 0
                btnPlay.isEnabled = true
                btnPause.isEnabled = false
                btnStop.isEnabled = false
            }
        }
    }

    private fun setupClickListeners() {
        btnSelectFile.setOnClickListener {
            isUrlMode = false
            urlInputLayout.visibility = android.view.View.GONE
            selectAudioFile()
        }

        btnLoadUrl.setOnClickListener {
            isUrlMode = true
            urlInputLayout.visibility = android.view.View.VISIBLE
        }

        btnLoadFromUrl.setOnClickListener {
            loadFromUrl()
        }

        btnPlay.setOnClickListener {
            playAudio()
        }

        btnPause.setOnClickListener {
            pauseAudio()
        }

        btnStop.setOnClickListener {
            stopAudio()
        }

        btnHistory.setOnClickListener {
            showTrackHistory()
        }
    }

    private fun selectAudioFile() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "audio/*"
        startActivityForResult(intent, REQUEST_CODE_AUDIO_FILE)
    }

    private fun loadFromUrl() {
        val url = etUrl.text.toString().trim()
        if (url.isEmpty()) {
            Toast.makeText(this, "Введите URL", Toast.LENGTH_SHORT).show()
            return
        }

        progressBar.visibility = android.view.View.VISIBLE
        tvStatus.text = "Статус: Загрузка..."

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val inputStream = URL(url).openStream()
                val tempFile = File.createTempFile("audio", ".mp3", cacheDir)
                tempFile.outputStream().use { output ->
                    inputStream.copyTo(output)
                }

                withContext(Dispatchers.Main) {
                    currentAudioFile = tempFile
                    currentAudioUri = null
                    isUrlMode = true

                    val trackName = "Онлайн: ${getUrlFileName(url)}"
                    tvTrackInfo.text = trackName
                    tvStatus.text = "Статус: Загружено"
                    progressBar.visibility = android.view.View.GONE

                    // Сохраняем в историю
                    saveTrackToHistory(trackName, url, "url")

                    // Подготавливаем медиаплеер
                    mediaPlayer.reset()
                    mediaPlayer.setDataSource(tempFile.absolutePath)
                    mediaPlayer.prepare()
                    seekBar.max = mediaPlayer.duration
                    seekBar.progress = 0

                    Toast.makeText(this@MainActivity, "Аудио загружено", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = android.view.View.GONE
                    tvStatus.text = "Статус: Ошибка загрузки"
                    Toast.makeText(this@MainActivity, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun playAudio() {
        val hasAudio = currentAudioUri != null || currentAudioFile != null

        if (!hasAudio) {
            Toast.makeText(this, "Сначала выберите аудио файл или загрузите из URL", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            if (!mediaPlayer.isPlaying) {
                // Если медиаплеер не подготовлен, подготавливаем его
                if (mediaPlayer.duration == 0) {
                    mediaPlayer.reset()

                    if (isUrlMode) {
                        currentAudioFile?.let { file ->
                            mediaPlayer.setDataSource(file.absolutePath)
                        }
                    } else {
                        currentAudioUri?.let { uri ->
                            mediaPlayer.setDataSource(this, uri)
                        }
                    }
                    mediaPlayer.prepare()
                    seekBar.max = mediaPlayer.duration
                }

                mediaPlayer.start()
                tvStatus.text = "Воспроизведение: ${formatTime(mediaPlayer.currentPosition)} / ${formatTime(mediaPlayer.duration)}"
                updateSeekBar?.let { handler.post(it) }

                btnPlay.isEnabled = false
                btnPause.isEnabled = true
                btnStop.isEnabled = true
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Ошибка воспроизведения: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    private fun pauseAudio() {
        if (mediaPlayer.isPlaying) {
            mediaPlayer.pause()
            tvStatus.text = "На паузе: ${formatTime(mediaPlayer.currentPosition)} / ${formatTime(mediaPlayer.duration)}"
            btnPlay.isEnabled = true
            btnPause.isEnabled = false
        }
    }

    private fun stopAudio() {
        if (mediaPlayer.isPlaying) {
            mediaPlayer.stop()
        }
        mediaPlayer.reset()
        tvStatus.text = "Статус: Остановлен"
        seekBar.progress = 0
        btnPlay.isEnabled = true
        btnPause.isEnabled = false
        btnStop.isEnabled = false
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_AUDIO_FILE && resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                try {
                    currentAudioUri = uri
                    currentAudioFile = null
                    isUrlMode = false

                    // Подготавливаем медиаплеер
                    mediaPlayer.reset()
                    mediaPlayer.setDataSource(this, uri)
                    mediaPlayer.prepare()

                    val trackName = getFileName(uri)
                    tvTrackInfo.text = trackName
                    tvStatus.text = "Файл загружен: ${formatTime(mediaPlayer.duration)}"
                    seekBar.max = mediaPlayer.duration
                    seekBar.progress = 0

                    // Сохраняем в историю
                    saveTrackToHistory(trackName, uri.toString(), "file")

                    Toast.makeText(this, "Файл загружен успешно", Toast.LENGTH_SHORT).show()

                } catch (e: Exception) {
                    Toast.makeText(this, "Ошибка загрузки файла: ${e.message}", Toast.LENGTH_LONG).show()
                    currentAudioUri = null
                }
            }
        }
    }

    private fun getFileName(uri: Uri): String {
        return try {
            var result = ""
            val cursor = contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val displayName = it.getString(it.getColumnIndexOrThrow("_display_name"))
                    result = displayName ?: "Неизвестный файл"
                }
            }
            result.ifEmpty { "Аудио файл" }
        } catch (e: Exception) {
            "Аудио файл"
        }
    }

    private fun getUrlFileName(url: String): String {
        return try {
            url.substringAfterLast('/').substringBefore('?').ifEmpty { "аудио" }
        } catch (e: Exception) {
            "аудио"
        }
    }

    private fun formatTime(milliseconds: Int): String {
        val totalSeconds = milliseconds / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    // История треков
    private fun loadTrackHistory() {
        val historyJson = sharedPreferences.getString("track_history", "[]")
        try {
            val jsonArray = JSONArray(historyJson)
            trackHistory.clear()
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                val track = Track(
                    name = jsonObject.getString("name"),
                    uri = jsonObject.getString("uri"),
                    type = jsonObject.getString("type"),
                    timestamp = jsonObject.getLong("timestamp")
                )
                trackHistory.add(track)
            }
            // Сортируем по времени добавления (новые сверху)
            trackHistory.sortByDescending { it.timestamp }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun saveTrackToHistory(name: String, uri: String, type: String) {
        val track = Track(name, uri, type)

        // Удаляем дубликаты
        trackHistory.removeAll { it.uri == uri }

        // Добавляем в начало
        trackHistory.add(0, track)

        // Ограничиваем историю 20 треками
        if (trackHistory.size > 20) {
            trackHistory.removeAt(trackHistory.size - 1)
        }

        saveHistoryToPreferences()
    }

    private fun saveHistoryToPreferences() {
        val jsonArray = JSONArray()
        trackHistory.forEach { track ->
            val jsonObject = JSONObject().apply {
                put("name", track.name)
                put("uri", track.uri)
                put("type", track.type)
                put("timestamp", track.timestamp)
            }
            jsonArray.put(jsonObject)
        }
        sharedPreferences.edit().putString("track_history", jsonArray.toString()).apply()
    }

    private fun showTrackHistory() {
        if (trackHistory.isEmpty()) {
            Toast.makeText(this, "История треков пуста", Toast.LENGTH_SHORT).show()
            return
        }

        val trackNames = trackHistory.map { it.name }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("История треков (${trackHistory.size})")
            .setItems(trackNames) { dialog, which ->
                val selectedTrack = trackHistory[which]
                loadTrackFromHistory(selectedTrack)
            }
            .setPositiveButton("Очистить историю") { dialog, which ->
                clearTrackHistory()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun loadTrackFromHistory(track: Track) {
        progressBar.visibility = android.view.View.VISIBLE
        tvStatus.text = "Загрузка из истории..."

        when (track.type) {
            "file" -> {
                // Для локальных файлов
                try {
                    val uri = Uri.parse(track.uri)
                    currentAudioUri = uri
                    currentAudioFile = null
                    isUrlMode = false

                    mediaPlayer.reset()
                    mediaPlayer.setDataSource(this, uri)
                    mediaPlayer.prepare()

                    tvTrackInfo.text = track.name
                    tvStatus.text = "Загружено из истории: ${formatTime(mediaPlayer.duration)}"
                    seekBar.max = mediaPlayer.duration
                    seekBar.progress = 0
                    progressBar.visibility = android.view.View.GONE

                    Toast.makeText(this, "Трек загружен из истории", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    progressBar.visibility = android.view.View.GONE
                    Toast.makeText(this, "Ошибка загрузки файла: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
            "url" -> {
                // Для URL - загружаем заново
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val inputStream = URL(track.uri).openStream()
                        val tempFile = File.createTempFile("audio", ".mp3", cacheDir)
                        tempFile.outputStream().use { output ->
                            inputStream.copyTo(output)
                        }

                        withContext(Dispatchers.Main) {
                            currentAudioFile = tempFile
                            currentAudioUri = null
                            isUrlMode = true

                            tvTrackInfo.text = track.name
                            tvStatus.text = "Загружено из истории"
                            progressBar.visibility = android.view.View.GONE

                            // Подготавливаем медиаплеер
                            mediaPlayer.reset()
                            mediaPlayer.setDataSource(tempFile.absolutePath)
                            mediaPlayer.prepare()
                            seekBar.max = mediaPlayer.duration
                            seekBar.progress = 0

                            Toast.makeText(this@MainActivity, "Трек загружен из истории", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            progressBar.visibility = android.view.View.GONE
                            Toast.makeText(this@MainActivity, "Ошибка загрузки URL: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }

    private fun clearTrackHistory() {
        trackHistory.clear()
        saveHistoryToPreferences()
        Toast.makeText(this, "История очищена", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer.release()
        updateSeekBar?.let { handler.removeCallbacks(it) }
    }

    companion object {
        private const val REQUEST_CODE_AUDIO_FILE = 1001
    }
}