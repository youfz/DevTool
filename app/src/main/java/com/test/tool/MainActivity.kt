package com.test.tool

import android.os.Bundle
import android.content.ClipData
import android.content.ClipboardManager
import android.graphics.Typeface
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.*
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.test.tool.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
 private lateinit var binding: ActivityMainBinding
 private val vm: MainViewModel by viewModels { object : ViewModelProvider.Factory {
  override fun <T : ViewModel> create(modelClass: Class<T>): T {
   @Suppress("UNCHECKED_CAST") return MainViewModel((application as DebugMateApp).container) as T
  }
 } }
 private val radio get() = vm.container.radio
 private var radioLog: TextView? = null
 private val permissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
  if(grants.values.all { it }) radio.discover(vm.classic)
  else Toast.makeText(this,R.string.permission_hint,Toast.LENGTH_LONG).show()
 }
 private fun discover(classic: Boolean) {
  vm.classic = classic
  vm.characteristicIndex = 0
  val needed = if(android.os.Build.VERSION.SDK_INT >= 31) arrayOf(android.Manifest.permission.BLUETOOTH_CONNECT,android.Manifest.permission.BLUETOOTH_SCAN) else arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION)
  if(needed.all { checkSelfPermission(it)==android.content.pm.PackageManager.PERMISSION_GRANTED }) radio.discover(classic)
  else permissions.launch(needed)
 }
 private fun radioPage() {
  val state = radio.state.value
  text(getString(when(state.status) { 1 -> R.string.scanning; 2 -> R.string.connecting; 3 -> R.string.connected; else -> R.string.no_device })+" "+state.address)
  if(state.error.isNotBlank()) text(getString(R.string.error,state.error))
  button(R.string.ble_scan) { discover(false) }
  button(R.string.paired) { discover(true) }
  text(getString(R.string.spp_hint))
  if(state.status != 0) button(R.string.disconnect) { radio.close() }
  if(state.status == 0 && state.peers.isEmpty()) text(getString(R.string.no_peer))
  for(peer in state.peers) {
   val entry = MaterialButton(this,null,com.google.android.material.R.attr.materialButtonOutlinedStyle)
   entry.text = listOfNotNull(peer.name,peer.address,peer.rssi?.let { "$it dBm" }).joinToString("  ")
   entry.setOnClickListener { radio.connect(peer,vm.classic) }
   binding.content.addView(entry)
  }
  if(state.status == 3) {
   val options = state.characteristics
   val index = vm.characteristicIndex.coerceIn(0,(options.size-1).coerceAtLeast(0))
   val c = options.getOrNull(index)
   if(options.isNotEmpty()) {
    val picker = Spinner(this)
    picker.adapter = ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,options.map { it.service.uuid.toString()+" / "+it.uuid })
    picker.setSelection(index); binding.content.addView(picker)
    picker.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
     override fun onNothingSelected(p: AdapterView<*>?) {}
     override fun onItemSelected(p: AdapterView<*>?,v: View?,i: Int,id: Long) { if(i!=vm.characteristicIndex) { vm.characteristicIndex=i; render(7) } }
    }
   }
   val props = c?.properties ?: 0
   if(props and 2 != 0) button(R.string.read_value) { radio.operate(c,0,byteArrayOf()) }
   if(props and 48 != 0) button(R.string.subscribe) { radio.operate(c,2,byteArrayOf()) }
   if(vm.classic || props and 12 != 0) {
    val hex = CheckBox(this).apply { setText(R.string.hex_mode); isChecked=vm.radioHex; setOnCheckedChangeListener { _,value -> vm.radioHex=value } }
    binding.content.addView(hex)
    input(R.string.radio_input,vm.radioDraft) { vm.radioDraft=it }
    button(R.string.radio_send) {
     runCatching {
      val bytes = if(vm.radioHex) vm.radioDraft.filterNot(Char::isWhitespace).let { s -> require(s.length%2==0); s.chunked(2).map { it.toInt(16).toByte() }.toByteArray() } else vm.radioDraft.toByteArray()
      radio.operate(c,1,bytes)
     }.onFailure { Toast.makeText(this,getString(R.string.error,it.message),Toast.LENGTH_SHORT).show() }
    }
   }
  }
  radioLog=text(state.log.joinToString("\n"),true)
 }
 private var localOutput: TextView?=null
 private fun filteredLog()=vm.localLog.lines.value.filter { it.contains(vm.logFilter,true) }.joinToString("\n")
 private var editor: JsonEditor? = null
 private var output: TextView? = null
 private var sending: MaterialButton? = null
 private var exportText = ""
 private val save = registerForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
  if(uri != null) lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
   runCatching { contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(exportText) } }
    .onFailure { runOnUiThread { Toast.makeText(this@MainActivity, getString(R.string.error, it.message), Toast.LENGTH_LONG).show() } }
  }
 }
 override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(savedInstanceState)
  binding = ActivityMainBinding.inflate(layoutInflater)
  setContentView(binding.root)
  ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
   val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
   view.setPadding(bars.left, bars.top, bars.right, bars.bottom); insets
  }
  val labels = listOf(R.string.home,R.string.tools,R.string.devices,R.string.terminal,R.string.history)
  val icons = listOf(R.drawable.dm_home,R.drawable.dm_tools,R.drawable.dm_devices,R.drawable.dm_terminal,R.drawable.dm_history)
  labels.forEachIndexed { i, label -> binding.navigation.menu.add(0,i, i,label).setIcon(icons[i]) }
  binding.navigation.setOnItemSelectedListener { vm.page.value = it.itemId; true }
  binding.toolbar.menu.add(R.string.settings).apply { setIcon(android.R.drawable.ic_menu_preferences); setShowAsAction(android.view.MenuItem.SHOW_AS_ACTION_ALWAYS); setOnMenuItemClickListener { vm.page.value = 8; true } }
  binding.toolbar.setNavigationIcon(android.R.drawable.ic_menu_revert)
  binding.toolbar.navigationContentDescription = getString(R.string.back)
  binding.toolbar.setNavigationOnClickListener { vm.page.value = 0 }
  onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
   override fun handleOnBackPressed() { if(vm.page.value != 0) vm.page.value = 0 else { isEnabled = false; onBackPressedDispatcher.onBackPressed() } }
  })
  lifecycleScope.launch { repeatOnLifecycle(Lifecycle.State.STARTED) {
   launch {
    var previous: com.test.tool.data.RadioRepository.State? = null
    radio.state.collect { state ->
     if(vm.page.value == 7) {
      if(previous?.copy(log=state.log) != state) render(7) else radioLog?.text = state.log.joinToString("\n")
     } else if(vm.page.value == 0 && previous?.status != state.status) render(0)
     previous=state
    }
   }
   launch { vm.page.collect { render(it) } }
   launch { vm.result.collect { if(vm.page.value==6) output?.text = it.ifBlank { getString(R.string.ready) } } }
   launch { vm.jsonResult.collect { if(vm.page.value==5) { output?.text=it.ifBlank { getString(R.string.ready) }; editor?.markError(it) } } }
   launch { vm.localLog.lines.collect { localOutput?.text=filteredLog() } }
   launch { vm.busy.collect { sending?.isEnabled = !it } }
   launch { vm.container.themes.mode.collect { AppCompatDelegate.setDefaultNightMode(it) } }
   launch { vm.container.history.entries.collect { if(vm.page.value == 4 || vm.page.value == 0) render(vm.page.value) } }
  } }
 }
 private fun text(value: String, mono: Boolean = false): TextView = TextView(this).apply {
  text = value; textSize = 16f; setPadding(0,16,0,16); setTextIsSelectable(true)
  if(mono) typeface = Typeface.MONOSPACE
  binding.content.addView(this)
 }
 private fun button(label: Int, action: () -> Unit): MaterialButton = MaterialButton(this).apply {
  setText(label); minHeight = (48 * resources.displayMetrics.density).toInt()
  binding.content.addView(this); setOnClickListener { action() }
 }
 private fun input(label: Int, value: String, changed: (String) -> Unit) = (if(label==R.string.json_input) JsonEditor(this).also { editor=it } else EditText(this)).apply {
  hint = getString(label); setText(value); minHeight = (56 * resources.displayMetrics.density).toInt()
  inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
  filters = arrayOf(android.text.InputFilter.LengthFilter(1024 * 1024))
  maxLines = 12; typeface = Typeface.MONOSPACE
  doAfterTextChanged { changed(it.toString()) }; binding.content.addView(this)
 }
 private fun render(page: Int) {
  localOutput=null; editor=null; radioLog=null; binding.content.removeAllViews(); output = null; sending = null
  val title = when(page) { 1 -> R.string.tools; 2 -> R.string.devices; 3 -> R.string.terminal; 4 -> R.string.history; 5 -> R.string.json; 6 -> R.string.api; 7 -> R.string.bluetooth; 8 -> R.string.settings; 9 -> R.string.adb; 10 -> R.string.hex; 11 -> R.string.base64; 12 -> R.string.crc; 13 -> R.string.timestamp; else -> R.string.app_name }
  binding.toolbar.setTitle(title)
  binding.toolbar.navigationIcon = if(page == 0) null else androidx.appcompat.content.res.AppCompatResources.getDrawable(this,android.R.drawable.ic_menu_revert)
  binding.navigation.visibility = if(page <= 4) View.VISIBLE else View.GONE
  if(page <= 4) binding.navigation.menu.findItem(page)?.isChecked = true
  when(page) {
   0,1 -> Workbench(binding.content) { vm.page.value = it }.show(page == 0, vm.container.history.entries.value,radio.state.value.status == 3)
   in 10..13 -> {
    val source = input(if(page == 13) R.string.time_hint else R.string.tool_input,vm.toolInputs[page].orEmpty()) { vm.toolInputs[page] = it }
    val result = text(vm.toolResults[page].orEmpty(),true)
    fun convert(reverse: Boolean) {
     lifecycleScope.launch {
      val value = source.text.toString()
      val converted = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
       runCatching { com.test.tool.data.Conversions.apply(page,value,reverse) }.getOrElse { getString(R.string.error,it.message) }
      }
      vm.toolResults[page] = converted
      result.text = converted
     }
    }
    button(R.string.encode) { convert(false) }
    if(page <= 11) button(R.string.decode) { convert(true) }
   }
   2 -> { text(getString(if(radio.state.value.status == 3) R.string.connected else R.string.no_device)); button(R.string.bluetooth) { vm.page.value = 7 }; button(R.string.adb) { vm.page.value = 9 } }
   9 -> { text(getString(R.string.hardware_pending)); text(getString(R.string.local_info, android.os.Build.BRAND,android.os.Build.MODEL,android.os.Build.VERSION.RELEASE))
    val memory=android.app.ActivityManager.MemoryInfo()
    getSystemService(android.app.ActivityManager::class.java)?.getMemoryInfo(memory)
    fun size(bytes:Long)=android.text.format.Formatter.formatFileSize(this,bytes)
    text(getString(R.string.memory_info,size(memory.availMem),size(memory.totalMem)))
    val disk=android.os.StatFs(filesDir.absolutePath)
    text(getString(R.string.storage_info,size(disk.availableBytes),size(disk.totalBytes)))
   }
   3 -> {
    text(getString(R.string.local_log_scope))
    input(R.string.log_filter,vm.logFilter) { vm.logFilter=it; localOutput?.text=filteredLog() }
    button(R.string.log_start) { vm.startLog() }
    button(R.string.log_pause) { vm.localLog.stop() }
    button(R.string.log_clear) {
     MaterialAlertDialogBuilder(this).setMessage(R.string.log_clear).setNegativeButton(R.string.cancel,null).setPositiveButton(R.string.confirm) { _,_ -> vm.localLog.clear() }.show()
    }
    localOutput=text(filteredLog().ifBlank { getString(R.string.ready) },true)
    button(R.string.export) {
     MaterialAlertDialogBuilder(this).setMessage(R.string.export_confirm).setNegativeButton(R.string.cancel,null).setPositiveButton(R.string.export) { _,_ -> exportText=filteredLog(); save.launch("DebugMate-logcat.txt") }.show()
    }
   }
   7 -> radioPage()
   4 -> {
    val list=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL }
    fun filter(query:String) {
     list.removeAllViews()
     val matches=vm.container.history.entries.value.filter { it.contains(query,ignoreCase=true) }
     for(entry in matches.ifEmpty { listOf(getString(R.string.empty)) }) {
      list.addView(TextView(this).apply {
       text=entry; setTextIsSelectable(true); setPadding(20,18,20,18)
       setBackgroundColor(getColor(R.color.dm_surface)); minHeight=(48*resources.displayMetrics.density).toInt()
      })
     }
    }
    input(R.string.filter_history,vm.historyFilter) { vm.historyFilter=it; filter(it) }
    binding.content.addView(list); filter(vm.historyFilter)
    button(R.string.export) {
     val preview=vm.container.history.entries.value.filter { it.contains(vm.historyFilter,true) }.joinToString("\n")
     MaterialAlertDialogBuilder(this).setTitle(R.string.export_confirm).setMessage(preview).setNegativeButton(R.string.cancel,null).setPositiveButton(R.string.export) { _,_ -> exportText=preview; save.launch("DebugMate-history.txt") }.show()
    }
    button(R.string.clear) { MaterialAlertDialogBuilder(this).setMessage(R.string.clear_question).setNegativeButton(R.string.cancel,null).setPositiveButton(R.string.confirm) { _,_ -> vm.container.history.clear() }.show() }
   }
   8 -> {
    text(getString(R.string.appearance)).setTypeface(null,Typeface.BOLD)
    val group=RadioGroup(this)
    val choices=listOf(R.string.dark to AppCompatDelegate.MODE_NIGHT_YES,R.string.light to AppCompatDelegate.MODE_NIGHT_NO,R.string.system to AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
    for((label,mode) in choices) {
     val choice=RadioButton(this).apply {
      id=View.generateViewId(); setText(label); minHeight=(56*resources.displayMetrics.density).toInt()
      setBackgroundColor(getColor(R.color.dm_surface))
     }
     group.addView(choice)
     if(mode==AppCompatDelegate.getDefaultNightMode()) group.check(choice.id)
     choice.setOnClickListener { lifecycleScope.launch { vm.container.themes.set(mode) } }
    }
    binding.content.addView(group)
    text(getString(R.string.timeout)); text(getString(R.string.privacy))
   }
   5 -> {
    val labels = listOf(R.string.format,R.string.validate,R.string.tree,R.string.compare)
    val tabs = com.google.android.material.tabs.TabLayout(this)
    labels.forEach { tabs.addTab(tabs.newTab().setText(it)) }
    binding.content.addView(tabs)
    tabs.getTabAt(vm.jsonTab)?.select()
    tabs.addOnTabSelectedListener(object : com.google.android.material.tabs.TabLayout.OnTabSelectedListener {
     override fun onTabSelected(tab: com.google.android.material.tabs.TabLayout.Tab) { vm.jsonTab = tab.position; render(5) }
     override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab) {}
     override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab) {}
    })
    input(R.string.json_input,vm.jsonInput.value) { vm.jsonInput.value = it }.apply { minLines = 7; gravity=android.view.Gravity.TOP; setBackgroundColor(getColor(R.color.dm_surface)) }
    if(vm.jsonTab == 3) input(R.string.json_other,vm.jsonOther.value) { vm.jsonOther.value = it }
    fun execute(action: Int) { vm.json(action,getString(R.string.valid),getString(R.string.same),getString(R.string.different)) }
    button(labels[vm.jsonTab]) { if(vm.jsonTab==2) browseTree() else execute(vm.jsonTab) }
    if(vm.jsonTab == 0) button(R.string.minify) { execute(4) }
    result()
   }
   6 -> {
    Spinner(this).apply {
     val methods = listOf("GET","POST","PUT","PATCH","DELETE")
     adapter = ArrayAdapter(this@MainActivity,android.R.layout.simple_spinner_dropdown_item,methods)
     setSelection(methods.indexOf(vm.method.value))
     onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
      override fun onItemSelected(p: AdapterView<*>?,v: View?,i: Int,id: Long) { vm.method.value = methods[i] }
      override fun onNothingSelected(p: AdapterView<*>?) {}
     }; binding.content.addView(this)
    }
    input(R.string.url,vm.url.value) { vm.url.value = it }
    val tabs=com.google.android.material.tabs.TabLayout(this)
    tabs.tabMode=com.google.android.material.tabs.TabLayout.MODE_SCROLLABLE
    resources.getStringArray(R.array.request_tabs).forEach { tabs.addTab(tabs.newTab().setText(it)) }
    binding.content.addView(tabs)
    tabs.getTabAt(vm.apiTab)?.select()
    tabs.addOnTabSelectedListener(object : com.google.android.material.tabs.TabLayout.OnTabSelectedListener {
     override fun onTabSelected(tab: com.google.android.material.tabs.TabLayout.Tab) { vm.apiTab=tab.position; render(6) }
     override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab) {}
     override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab) {}
    })
    when(vm.apiTab) {
     0 -> input(R.string.query_params,vm.params) { vm.params=it }
     1 -> input(R.string.headers,vm.headers.value) { vm.headers.value=it }
     2 -> input(R.string.body,vm.body.value) { vm.body.value=it }.apply { minLines=5 }
     3 -> input(R.string.auth_token,vm.bearer) { vm.bearer=it }.apply { inputType=129 }
     4 -> input(R.string.env_vars,vm.environment) { vm.environment=it }
    }
    sending = button(R.string.send) { vm.send { status, elapsed, headers, body -> getString(R.string.http_result,status,elapsed,headers,body) } }.apply { isEnabled = !vm.busy.value }
    button(R.string.cancel) { vm.cancel(getString(R.string.cancel)) }; text(getString(R.string.timeout)); result()
   }
  }
 }
 private fun browseTree() {
  val source=vm.jsonInput.value
  lifecycleScope.launch {
   val root=kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) { runCatching { vm.container.json.parse(source) } }
   root.onFailure { Toast.makeText(this@MainActivity,getString(R.string.error,it.message),Toast.LENGTH_LONG).show() }
   root.onSuccess { showNode(getString(R.string.tree_root),it,null) }
  }
 }
 private fun showNode(title: String,node: com.google.gson.JsonElement,back: (() -> Unit)?) {
  val dialog=MaterialAlertDialogBuilder(this).setTitle(title).setNegativeButton(R.string.done,null)
  if(back!=null) dialog.setNeutralButton(R.string.up_level) { _,_ -> back() }
  val children = when {
   node.isJsonObject -> node.asJsonObject.entrySet().map { it.key to it.value }
   node.isJsonArray -> node.asJsonArray.mapIndexed { i,value -> "[$i]" to value }
   else -> emptyList()
  }
  if(children.isEmpty()) dialog.setMessage(node.toString())
  else {
   val adapter=object : BaseAdapter() {
    override fun getCount()=children.size
    override fun getItem(position:Int)=children[position]
    override fun getItemId(position:Int)=position.toLong()
    override fun getView(position:Int,recycled:View?,parent:android.view.ViewGroup?):View {
     val row=recycled as? TextView ?: TextView(this@MainActivity).apply { minHeight=(48*resources.displayMetrics.density).toInt(); gravity=android.view.Gravity.CENTER_VERTICAL; setPadding(24,0,24,0); typeface=Typeface.MONOSPACE }
     val (key,value)=children[position]
     row.text=key+"  "+when { value.isJsonObject -> "{ "+value.asJsonObject.size()+" }  ›"; value.isJsonArray -> "[ "+value.asJsonArray.size()+" ]  ›"; else -> value.toString().take(120) }
     return row
    }
   }
   dialog.setAdapter(adapter) { _,index -> val (key,value)=children[index]; showNode(key,value) { showNode(title,node,back) } }
  }
  dialog.show()
 }
 private fun result() {
  val result = if(vm.page.value==5) vm.jsonResult else vm.result
  output = text(result.value.ifBlank { getString(R.string.ready) }, true)
  button(R.string.copy) { (getSystemService(CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText(getString(R.string.app_name),result.value)) }
  button(R.string.export) { MaterialAlertDialogBuilder(this).setMessage(R.string.export_confirm).setNegativeButton(R.string.cancel,null).setPositiveButton(R.string.confirm) { _,_ -> exportText = result.value; save.launch("DebugMate.txt") }.show() }
 }
}









