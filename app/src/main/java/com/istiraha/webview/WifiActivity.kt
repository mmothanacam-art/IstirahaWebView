package com.istiraha.app
import com.istiraha.webview.R
import android.Manifest
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSuggestion
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class WifiActivity : AppCompatActivity() {

    private lateinit var wifiManager: WifiManager

    private lateinit var statusText: TextView
    private lateinit var strongestText: TextView
    private lateinit var wifiList: ListView
    private lateinit var scanButton: Button

    private var receiverRegistered = false
    private var currentNetworks: List<ScanResult> = emptyList()

    private val permissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) {
            if (hasRequiredPermissions()) {
                checkLocationAndScan()
            } else {
                statusText.text = "يجب السماح بأذونات WiFi والموقع"

                Toast.makeText(
                    this,
                    "اسمح بأذونات WiFi والموقع ثم حاول مرة أخرى",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    private val wifiScanReceiver =
        object : BroadcastReceiver() {

            override fun onReceive(
                context: Context?,
                intent: Intent?
            ) {
                if (
                    intent?.action ==
                    WifiManager.SCAN_RESULTS_AVAILABLE_ACTION
                ) {
                    showScanResults()
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_wifi)

        wifiManager =
            applicationContext.getSystemService(
                Context.WIFI_SERVICE
            ) as WifiManager

        statusText = findViewById(R.id.statusText)
        strongestText = findViewById(R.id.strongestText)
        wifiList = findViewById(R.id.wifiList)
        scanButton = findViewById(R.id.scanButton)

        registerWifiReceiver()

        scanButton.setOnClickListener {
            checkPermissions()
        }

        wifiList.setOnItemClickListener { _, _, position, _ ->

            if (position in currentNetworks.indices) {
                showNetworkDialog(
                    currentNetworks[position]
                )
            }
        }

        statusText.text = "جاهز لفحص شبكات WiFi"
    }

    private fun checkPermissions() {

        if (hasRequiredPermissions()) {
            checkLocationAndScan()
            return
        }

        val permissions =
            mutableListOf(
                Manifest.permission.ACCESS_FINE_LOCATION
            )

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.TIRAMISU
        ) {
            permissions.add(
                Manifest.permission.NEARBY_WIFI_DEVICES
            )
        }

        permissionLauncher.launch(
            permissions.toTypedArray()
        )
    }

    private fun hasRequiredPermissions(): Boolean {

        val locationGranted =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        if (!locationGranted) {
            return false
        }

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.TIRAMISU
        ) {

            val nearbyGranted =
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.NEARBY_WIFI_DEVICES
                ) == PackageManager.PERMISSION_GRANTED

            if (!nearbyGranted) {
                return false
            }
        }

        return true
    }

    private fun checkLocationAndScan() {

        if (!isLocationEnabled()) {

            statusText.text = "خدمة الموقع مغلقة"

            Toast.makeText(
                this,
                "قم بتشغيل الموقع حتى يستطيع التطبيق فحص WiFi",
                Toast.LENGTH_LONG
            ).show()

            try {
                startActivity(
                    Intent(
                        Settings.ACTION_LOCATION_SOURCE_SETTINGS
                    )
                )
            } catch (_: Exception) {
            }

            return
        }

        startWifiScan()
    }

    private fun isLocationEnabled(): Boolean {

        val locationManager =
            getSystemService(
                Context.LOCATION_SERVICE
            ) as LocationManager

        return try {

            locationManager.isProviderEnabled(
                LocationManager.GPS_PROVIDER
            ) ||
                    locationManager.isProviderEnabled(
                        LocationManager.NETWORK_PROVIDER
                    )

        } catch (_: Exception) {
            false
        }
    }

    private fun startWifiScan() {

        if (!wifiManager.isWifiEnabled) {

            statusText.text = "WiFi غير مُفعّل"

            Toast.makeText(
                this,
                "قم بتشغيل WiFi أولاً",
                Toast.LENGTH_LONG
            ).show()

            return
        }

        statusText.text = "جاري فحص شبكات WiFi..."

        try {

            val started =
                wifiManager.startScan()

            if (!started) {
                showScanResults()

                statusText.text =
                    "تم عرض آخر نتائج WiFi المتاحة"
            }

        } catch (_: SecurityException) {

            statusText.text =
                "التطبيق لا يملك صلاحية فحص WiFi"
        }
    }

    private fun showScanResults() {

        if (!hasRequiredPermissions()) {
            return
        }

        val results: List<ScanResult>

        try {
            results = wifiManager.scanResults
        } catch (_: SecurityException) {

            statusText.text =
                "تعذر قراءة شبكات WiFi"

            return
        }

        val networkComparator =
            compareByDescending<ScanResult> {
                getQualityPriority(it.level)
            }
                .thenByDescending {
                    getBandPriority(it.frequency)
                }
                .thenByDescending {
                    it.level
                }

        currentNetworks =
            results
                .filter {
                    it.SSID.isNotBlank()
                }
                .groupBy {
                    it.SSID
                }
                .mapNotNull { (_, accessPoints) ->

                    accessPoints
                        .sortedWith(networkComparator)
                        .firstOrNull()
                }
                .sortedWith(networkComparator)

        if (currentNetworks.isEmpty()) {

            strongestText.text =
                "أفضل شبكة: --"

            wifiList.adapter =
                ArrayAdapter(
                    this,
                    android.R.layout.simple_list_item_1,
                    emptyList<String>()
                )

            statusText.text =
                "لم يتم العثور على شبكات WiFi"

            return
        }

        val strongest =
            currentNetworks.first()

        strongestText.text =
            "أفضل شبكة: " +
                    "${getSignalIcon(strongest.level)} " +
                    "${strongest.SSID} " +
                    "(${strongest.level} dBm)"

        val items =
            currentNetworks.mapIndexed { index, network ->

                """
                ${index + 1}. ${network.SSID}
                ${getSignalIcon(network.level)} ${network.level} dBm • ${getBand(network.frequency)} • ${getQuality(network.level)}
                """.trimIndent()
            }

        wifiList.adapter =
            ArrayAdapter(
                this,
                android.R.layout.simple_list_item_1,
                items
            )

        statusText.text =
            "تم العثور على ${currentNetworks.size} شبكة"
    }

    private fun getQualityPriority(level: Int): Int {

        return when {
            level >= -50 -> 5
            level >= -60 -> 4
            level >= -70 -> 3
            level >= -80 -> 2
            else -> 1
        }
    }

    private fun getQuality(level: Int): String {

        return when {
            level >= -50 -> "قوية جداً"
            level >= -60 -> "ممتازة"
            level >= -70 -> "جيدة"
            level >= -80 -> "ضعيفة"
            else -> "ضعيفة جداً"
        }
    }

    private fun getSignalIcon(level: Int): String {

        return when {
            level >= -60 -> "🟢"
            level >= -70 -> "🟡"
            else -> "🔴"
        }
    }

    private fun getBandPriority(frequency: Int): Int {

        return when {
            frequency >= 5925 -> 3
            frequency >= 4900 -> 2
            else -> 1
        }
    }

    private fun getBand(frequency: Int): String {

        return when {
            frequency >= 5925 -> "6 GHz"
            frequency >= 4900 -> "5 GHz"
            else -> "2.4 GHz"
        }
    }

    private fun showNetworkDialog(
        network: ScanResult
    ) {

        val secured =
            isSecured(network)

        val securityText =
            if (secured) {
                "محمية بكلمة مرور"
            } else {
                "شبكة مفتوحة"
            }

        val message =
            """
            الشبكة: ${network.SSID}

            ${getSignalIcon(network.level)} قوة الإشارة: ${network.level} dBm
            التردد: ${getBand(network.frequency)}
            التقييم: ${getQuality(network.level)}
            الحماية: $securityText
            """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle("الاتصال بالشبكة")
            .setMessage(message)
            .setPositiveButton("اتصال") { _, _ ->

                if (secured) {
                    showPasswordDialog(network)
                } else {
                    connectUsingSuggestion(
                        network,
                        null
                    )
                }
            }
            .setNegativeButton(
                "إلغاء",
                null
            )
            .show()
    }

    private fun isSecured(
        network: ScanResult
    ): Boolean {

        val capabilities =
            network.capabilities.uppercase()

        return capabilities.contains("WPA") ||
                capabilities.contains("WEP") ||
                capabilities.contains("SAE") ||
                capabilities.contains("PSK")
    }

    private fun showPasswordDialog(
        network: ScanResult
    ) {

        val passwordInput =
            EditText(this)

        passwordInput.hint =
            "كلمة مرور WiFi"

        passwordInput.inputType =
            InputType.TYPE_CLASS_TEXT or
                    InputType.TYPE_TEXT_VARIATION_PASSWORD

        passwordInput.setPadding(
            50,
            25,
            50,
            25
        )

        AlertDialog.Builder(this)
            .setTitle(network.SSID)
            .setMessage(
                "أدخل كلمة مرور الشبكة"
            )
            .setView(passwordInput)
            .setPositiveButton("اتصال") { _, _ ->

                val password =
                    passwordInput.text.toString()

                if (password.length < 8) {

                    Toast.makeText(
                        this,
                        "كلمة المرور يجب أن تكون 8 أحرف على الأقل",
                        Toast.LENGTH_LONG
                    ).show()

                } else {

                    connectUsingSuggestion(
                        network,
                        password
                    )
                }
            }
            .setNegativeButton(
                "إلغاء",
                null
            )
            .show()
    }

    private fun connectUsingSuggestion(
        scanResult: ScanResult,
        password: String?
    ) {

        if (
            Build.VERSION.SDK_INT <
            Build.VERSION_CODES.Q
        ) {

            Toast.makeText(
                this,
                "الاتصال يتطلب Android 10 أو أحدث",
                Toast.LENGTH_LONG
            ).show()

            return
        }

        try {

            val capabilities =
                scanResult.capabilities.uppercase()

            if (
                capabilities.contains("WEP")
            ) {

                Toast.makeText(
                    this,
                    "شبكات WEP القديمة غير مدعومة حالياً",
                    Toast.LENGTH_LONG
                ).show()

                return
            }

            val builder =
                WifiNetworkSuggestion
                    .Builder()
                    .setSsid(
                        scanResult.SSID
                    )

            if (
                !password.isNullOrBlank()
            ) {

                if (
                    capabilities.contains("SAE")
                ) {

                    builder.setWpa3Passphrase(
                        password
                    )

                } else {

                    builder.setWpa2Passphrase(
                        password
                    )
                }
            }

            val suggestion =
                builder.build()

            val result =
                wifiManager.addNetworkSuggestions(
                    listOf(suggestion)
                )

            when (result) {

                WifiManager.STATUS_NETWORK_SUGGESTIONS_SUCCESS -> {

                    statusText.text =
                        "تم إرسال ${scanResult.SSID} إلى Android"

                    Toast.makeText(
                        this,
                        "تمت إضافة الشبكة. وافق على اقتراح WiFi إذا طلب Android ذلك.",
                        Toast.LENGTH_LONG
                    ).show()
                }

                WifiManager.STATUS_NETWORK_SUGGESTIONS_ERROR_ADD_DUPLICATE -> {

                    statusText.text =
                        "${scanResult.SSID} مضافة مسبقاً"

                    Toast.makeText(
                        this,
                        "هذه الشبكة مضافة مسبقاً",
                        Toast.LENGTH_LONG
                    ).show()
                }

                WifiManager.STATUS_NETWORK_SUGGESTIONS_ERROR_APP_DISALLOWED -> {

                    statusText.text =
                        "Android لا يسمح باقتراح الشبكات حالياً"

                    Toast.makeText(
                        this,
                        "يجب السماح لتطبيق الاستراحة باقتراح شبكات WiFi",
                        Toast.LENGTH_LONG
                    ).show()

                    openWifiSettings()
                }

                else -> {

                    statusText.text =
                        "تعذر إضافة الشبكة — الخطأ: $result"

                    Toast.makeText(
                        this,
                        "تعذر إضافة الشبكة. رمز الخطأ: $result",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

        } catch (e: SecurityException) {

            statusText.text =
                "خطأ في صلاحيات WiFi"

            Toast.makeText(
                this,
                e.message ?: "تحقق من صلاحيات WiFi",
                Toast.LENGTH_LONG
            ).show()

        } catch (e: Exception) {

            statusText.text =
                "حدث خطأ أثناء إعداد الشبكة"

            Toast.makeText(
                this,
                "خطأ: ${e.message ?: "غير معروف"}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun openWifiSettings() {

        try {

            startActivity(
                Intent(
                    Settings.ACTION_WIFI_SETTINGS
                )
            )

        } catch (_: Exception) {

            try {
                startActivity(
                    Intent(
                        Settings.ACTION_SETTINGS
                    )
                )
            } catch (_: Exception) {
            }
        }
    }

    private fun registerWifiReceiver() {

        if (receiverRegistered) {
            return
        }

        val filter =
            IntentFilter(
                WifiManager.SCAN_RESULTS_AVAILABLE_ACTION
            )

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.TIRAMISU
        ) {

            registerReceiver(
                wifiScanReceiver,
                filter,
                RECEIVER_NOT_EXPORTED
            )

        } else {

            registerReceiver(
                wifiScanReceiver,
                filter
            )
        }

        receiverRegistered = true
    }

    override fun onDestroy() {

        if (receiverRegistered) {

            try {
                unregisterReceiver(
                    wifiScanReceiver
                )
            } catch (_: Exception) {
            }

            receiverRegistered = false
        }

        super.onDestroy()
    }
}
