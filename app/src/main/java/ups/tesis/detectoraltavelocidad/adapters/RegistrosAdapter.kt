import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import ups.tesis.detectoraltavelocidad.R
import ups.tesis.detectoraltavelocidad.conexionec2.models.showRegs

class RegistroAdapter(private val registroList: List<showRegs>) :
    RecyclerView.Adapter<RegistroAdapter.RegistroViewHolder>() {

    inner class RegistroViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDireccion: TextView = view.findViewById(R.id.tvDireccion)
        val tvVelocidad: TextView = view.findViewById(R.id.tvVelocidad)
        val tvVelocidadPermitida: TextView = view.findViewById(R.id.tvVelocidadPermitida)
        val tvFecha: TextView = view.findViewById(R.id.tvFecha)
        val tvId: TextView = view.findViewById(R.id.tvId)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RegistroViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.style_speed_data, parent, false)
        return RegistroViewHolder(view)
    }

    override fun onBindViewHolder(holder: RegistroViewHolder, position: Int) {
        val registro = registroList[position]
        holder.tvDireccion.text = "Dirección: ${registro.direccion}"
        holder.tvVelocidad.text = "Velocidad: ${registro.velocidad}"
        holder.tvVelocidadPermitida.text = "Velocidad Permitida: ${registro.street_max_speed}"
        holder.tvFecha.text = "Fecha: ${registro.fecha}"
        holder.tvId.text = "ID: ${registro.id}"
    }

    override fun getItemCount(): Int = registroList.size
}

class LocalDataAdapter(private val registroList: List<showRegs>) :
    RecyclerView.Adapter<LocalDataAdapter.LocalDataViewHolder>() {

    inner class LocalDataViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDireccion: TextView = view.findViewById(R.id.tvDireccion)
        val tvVelocidad: TextView = view.findViewById(R.id.tvVelocidad)
        val tvVelocidadPermitida: TextView = view.findViewById(R.id.tvVelocidadPermitida)
        val tvFecha: TextView = view.findViewById(R.id.tvFecha)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LocalDataViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.style_speed_data, parent, false)
        return LocalDataViewHolder(view)
    }

    override fun onBindViewHolder(holder: LocalDataViewHolder, position: Int) {
        val registro = registroList[position]
        holder.tvDireccion.text = "Dirección: ${registro.direccion}"
        holder.tvVelocidad.text = "Velocidad: ${registro.velocidad}"
        holder.tvVelocidadPermitida.text = "Velocidad Permitida: ${registro.street_max_speed}"
        holder.tvFecha.text = "Fecha: ${registro.fecha}"
    }

    override fun getItemCount(): Int = registroList.size
}