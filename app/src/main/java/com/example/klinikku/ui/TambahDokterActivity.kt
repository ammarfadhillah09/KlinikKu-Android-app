package com.example.klinikku.ui

import android.app.AlertDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.klinikku.R
import com.example.klinikku.data.model.Dokter
import com.example.klinikku.data.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

class TambahDokterActivity : AppCompatActivity() {

    // Stored when the activity is launched in Edit Mode
    private var idJadwal: String = ""
    private var isEditMode: Boolean = false
    private var nikOriginal: String = ""

    // Tracks whether the admin has chosen to modify login credentials
    private var isCredentialModified: Boolean = false

    // Multi-choice day selection data
    private val listHari = arrayOf("Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu", "Minggu")
    private val checkedHari = BooleanArray(7)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tambah_dokter)

        // === View Bindings ===
        val tvTitle      = findViewById<TextView>(R.id.tvFormTitle)
        val etNik        = findViewById<EditText>(R.id.etNikDokter)
        val etNama       = findViewById<EditText>(R.id.etNamaDokter)
        val spinnerPoli  = findViewById<Spinner>(R.id.spinnerPoliDokter)
        val etSpesialis  = findViewById<EditText>(R.id.etSpesialisDokter)
        val etUsername   = findViewById<EditText>(R.id.etUsernameDokter)
        val etPassword   = findViewById<EditText>(R.id.etPasswordDokter)
        val etHariPraktek = findViewById<EditText>(R.id.etHariPraktek)
        val etJamMulai   = findViewById<EditText>(R.id.etJamMulai)
        val etJamSelesai = findViewById<EditText>(R.id.etJamSelesai)
        val btnSimpan         = findViewById<Button>(R.id.btnSimpanDokter)
        val btnUbahKredensial = findViewById<Button>(R.id.btnUbahKredensial)

        // === Load Poli Spinner ===
        loadPoliSpinner(spinnerPoli)

        // === Setup Hari Praktek – Multi-choice AlertDialog ===
        etHariPraktek.setOnClickListener {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Pilih Hari Praktek")
            builder.setMultiChoiceItems(listHari, checkedHari) { _, which, isChecked ->
                checkedHari[which] = isChecked
            }
            builder.setPositiveButton("OK") { _, _ ->
                val selectedDays = mutableListOf<String>()
                for (i in listHari.indices) {
                    if (checkedHari[i]) selectedDays.add(listHari[i])
                }
                etHariPraktek.setText(selectedDays.joinToString(","))
            }
            builder.setNegativeButton("Batal", null)
            builder.show()
        }

        // === Time Picker Listeners ===
        fun showTimePicker(target: EditText) {
            val cal = Calendar.getInstance()
            TimePickerDialog(
                this,
                { _, hour, minute -> target.setText(String.format("%02d:%02d", hour, minute)) },
                cal.get(Calendar.HOUR_OF_DAY),
                cal.get(Calendar.MINUTE),
                true  // 24-hour clock
            ).show()
        }
        etJamMulai.setOnClickListener   { showTimePicker(etJamMulai) }
        etJamSelesai.setOnClickListener { showTimePicker(etJamSelesai) }

        // ===================================================================
        // TASK 2: Check for Edit Mode
        // ===================================================================
        if (intent.hasExtra("EDIT_MODE_DOKTER")) {
            isEditMode   = true
            nikOriginal  = intent.getStringExtra("DOKTER_NIK") ?: ""
            idJadwal     = intent.getStringExtra("DOKTER_ID_JADWAL") ?: ""

            // Update UI for edit mode
            tvTitle.text    = "Edit Data Dokter"
            btnSimpan.text  = "UPDATE DATA DOKTER"
            btnSimpan.backgroundTintList =
                android.content.res.ColorStateList.valueOf(
                    android.graphics.Color.parseColor("#0984E3")
                )

            // Populate all fields with existing data
            etNik.setText(nikOriginal)
            etNik.isEnabled = false  // NIK is the primary key – should not be changed
            etNama.setText(intent.getStringExtra("DOKTER_NAMA") ?: "")
            etSpesialis.setText(intent.getStringExtra("DOKTER_SPESIALIS") ?: "")

            // Pre-populate multi-choice day field
            val passedDays = intent.getStringExtra("DOKTER_HARI") ?: ""
            etHariPraktek.setText(passedDays)
            // Pre-check matching items in the BooleanArray so the dialog reflects existing data
            val daysList = passedDays.split(",").map { it.trim().lowercase() }
            for (i in listHari.indices) {
                checkedHari[i] = daysList.contains(listHari[i].lowercase())
            }

            etJamMulai.setText(intent.getStringExtra("DOKTER_JAM_MULAI") ?: "")
            etJamSelesai.setText(intent.getStringExtra("DOKTER_JAM_SELESAI") ?: "")

            // --- Credential fields: pre-fill and lock by default ---
            // Explicitly cache the original username so both the initial display
            // and the "Batal" revert both use the exact same value.
            val currentUsername = intent.getStringExtra("DOKTER_USERNAME") ?: ""
            etUsername.setText(currentUsername)
            etPassword.setText("********")  // Placeholder – actual value stays in DB
            etUsername.isEnabled = false
            etPassword.isEnabled = false
            Log.d("EDIT_DOKTER", "Loaded username: '$currentUsername', NIK: '$nikOriginal'")

            // Show the credential toggle button
            btnUbahKredensial.visibility = android.view.View.VISIBLE

            // Toggle logic: enable/disable credential fields
            btnUbahKredensial.setOnClickListener {
                if (!isCredentialModified) {
                    // Unlock: allow admin to type new credentials
                    isCredentialModified = true
                    etUsername.isEnabled = true
                    etPassword.isEnabled = true
                    etPassword.setText("")  // Clear placeholder so admin types a fresh password
                    etUsername.requestFocus()
                    btnUbahKredensial.text = "Batal Ubah Kredensial"
                    btnUbahKredensial.backgroundTintList =
                        android.content.res.ColorStateList.valueOf(
                            android.graphics.Color.parseColor("#FDEDED")
                        )
                    btnUbahKredensial.setTextColor(
                        android.graphics.Color.parseColor("#D63031")
                    )
                } else {
                    // Lock: revert to read-only display using the cached username
                    isCredentialModified = false
                    etUsername.setText(currentUsername)
                    etPassword.setText("********")
                    etUsername.isEnabled = false
                    etPassword.isEnabled = false
                    btnUbahKredensial.text = "Ubah Kredensial"
                    btnUbahKredensial.backgroundTintList =
                        android.content.res.ColorStateList.valueOf(
                            android.graphics.Color.parseColor("#E8F4FD")
                        )
                    btnUbahKredensial.setTextColor(
                        android.graphics.Color.parseColor("#0984E3")
                    )
                }
            }

            // Pre-select poli spinner after it loads
            val targetPoli = intent.getStringExtra("DOKTER_POLI") ?: ""
            spinnerPoli.post {
                val adapter = spinnerPoli.adapter ?: return@post
                for (i in 0 until adapter.count) {
                    if (adapter.getItem(i).toString() == targetPoli) {
                        spinnerPoli.setSelection(i)
                        break
                    }
                }
            }
        }

        // ===================================================================
        // Submit Button Click Handler
        // ===================================================================
        btnSimpan.setOnClickListener {
            val nik        = etNik.text.toString().trim()
            val nama       = etNama.text.toString().trim()
            val poli       = spinnerPoli.selectedItem?.toString() ?: ""
            val spesialis  = etSpesialis.text.toString().trim()
            val user       = etUsername.text.toString().trim()
            val pass       = etPassword.text.toString().trim()
            val hari       = etHariPraktek.text.toString().trim()
            val jamMulai   = etJamMulai.text.toString().trim()
            val jamSelesai = etJamSelesai.text.toString().trim()

            // 1. Validate Input
            if (!isEditMode && nik.length != 16) {
                Toast.makeText(this, "NIK harus 16 digit", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // In edit mode, only validate credentials if the admin chose to modify them
            val credentialsRequired = !isEditMode || isCredentialModified
            if (credentialsRequired && (user.isEmpty() || pass.isEmpty())) {
                Toast.makeText(this, "Username dan Password tidak boleh kosong", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (nama.isEmpty() || spesialis.isEmpty() || hari.isEmpty() ||
                jamMulai.isEmpty() || jamSelesai.isEmpty()
            ) {
                Toast.makeText(this, "Harap lengkapi semua field", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 2. Build the unified payload
            // If credentials were NOT modified in edit mode, send empty strings
            // so the backend knows to skip credential updates.
            val finalUsername = if (isEditMode && !isCredentialModified) "" else user
            val finalPassword = if (isEditMode && !isCredentialModified) "" else pass

            val payload = Dokter(
                nik         = if (isEditMode) nikOriginal else nik,
                nama        = nama,
                poli        = poli,
                spesialis   = spesialis,
                username    = finalUsername,
                password    = finalPassword,
                hari        = hari,
                jam_mulai   = jamMulai,
                jam_selesai = jamSelesai,
                id_jadwal   = idJadwal
            )

            // 3. Dispatch POST or PUT
            // For Edit Mode: derive the NIK path-param directly from the intent extra
            // (with etNik text as fallback) so the URL path is never accidentally empty.
            if (isEditMode) {
                val nikDokter = intent.getStringExtra("DOKTER_NIK")
                    ?: etNik.text.toString().trim()
                Log.d("EDIT_DOKTER", "PUT /api/admin/dokter/$nikDokter")
                prosesUpdateDokter(nik = nikDokter, dokter = payload, btn = btnSimpan)
            } else {
                prosesSimpanDokter(payload, btnSimpan)
            }
        }
    }

    // ===================================================================
    // Load Poli Spinner from API with fallback
    // ===================================================================
    private fun loadPoliSpinner(spinner: Spinner) {
        lifecycleScope.launch {
            val poliNames = try {
                val response = RetrofitClient.instance.getPoli()
                if (response.isSuccessful) {
                    val names = response.body()?.data?.map { it.nama_poli } ?: listOf()
                    if (names.isNotEmpty()) names else listOf("Umum", "Gigi", "Anak")
                } else {
                    listOf("Umum", "Gigi", "Anak")
                }
            } catch (e: Exception) {
                Log.e("TAMBAH_DOKTER", "Gagal memuat poli: ${e.message}")
                listOf("Umum", "Gigi", "Anak")
            }

            val adapter = ArrayAdapter(
                this@TambahDokterActivity,
                android.R.layout.simple_spinner_item,
                poliNames
            )
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinner.adapter = adapter
        }
    }

    // ===================================================================
    // TASK 1: POST – Create new Doctor
    // ===================================================================
    private fun prosesSimpanDokter(dokter: Dokter, btn: Button) {
        btn.isEnabled = false
        btn.text = "Memproses..."

        lifecycleScope.launch(Dispatchers.Main) {
            try {
                val response = RetrofitClient.instance.addDokter(dokter)
                if (response.isSuccessful) {
                    Toast.makeText(
                        this@TambahDokterActivity,
                        "Akun Dokter Berhasil Dibuat",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Gagal membuat akun dokter"
                    Toast.makeText(this@TambahDokterActivity, errorMsg, Toast.LENGTH_LONG).show()
                    Log.e("API_ERROR", errorMsg)
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@TambahDokterActivity,
                    "Koneksi Error: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
                Log.e("API_ERROR", "Exception addDokter: ${e.message}")
            } finally {
                btn.isEnabled = true
                btn.text = "BUAT AKUN DOKTER"
            }
        }
    }

    // ===================================================================
    // TASK 2: PUT – Update existing Doctor
    // nik is passed explicitly as a separate parameter (not taken from dokter.nik)
    // to guarantee the @Path("nik") value in the URL is never empty.
    // ===================================================================
    private fun prosesUpdateDokter(nik: String, dokter: Dokter, btn: Button) {
        btn.isEnabled = false
        btn.text = "Memperbarui..."

        lifecycleScope.launch(Dispatchers.Main) {
            try {
                val response = RetrofitClient.instance.updateDokter(
                    nik     = nik,
                    dokter  = dokter
                )
                if (response.isSuccessful) {
                    Toast.makeText(
                        this@TambahDokterActivity,
                        "Data Dokter Berhasil Diperbarui",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Gagal memperbarui data dokter"
                    Toast.makeText(this@TambahDokterActivity, errorMsg, Toast.LENGTH_LONG).show()
                    Log.e("API_ERROR", "updateDokter error: $errorMsg")
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@TambahDokterActivity,
                    "Koneksi Error: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
                Log.e("API_ERROR", "Exception updateDokter: ${e.message}")
            } finally {
                btn.isEnabled = true
                btn.text = "UPDATE DATA DOKTER"
            }
        }
    }
}
