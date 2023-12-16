package com.bolsadeideas.springboot.app.controllers;

import com.bolsadeideas.springboot.app.models.entity.Cliente;
import com.bolsadeideas.springboot.app.models.services.IClienteService;
import com.bolsadeideas.springboot.app.models.services.IUploadFileService;
import com.bolsadeideas.springboot.app.util.paginator.PageRender;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Map;

@Controller
// crea una sesión con los datos del objeto cliente y los pasas a la vista
@SessionAttributes("cliente")
public class ClienteController {


//    @Qualifier("clienteDaoJPA") // sirve en caso de que se inyecte IClienteDao en dos o mas componentes
    @Autowired
    private IClienteService clienteService;

    @Autowired
    private IUploadFileService uploadFileService;

    public ResponseEntity<Resource> verFoto(@PathVariable String filename) {

        Resource recurso = null;
        try{
             recurso =  uploadFileService.load(filename);
        }catch (MalformedURLException e){
            e.printStackTrace();
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\""+ recurso.getFilename() + "\"")
                .body(recurso);
    }

    @GetMapping(value = "/ver/{id}")
    public String detalles(@PathVariable(name = "id") Long id, Map<String, Object> model, RedirectAttributes flash){

        Cliente cliente = clienteService.fetchByIdWithFacturas(id);
//      findOne(id);

        if(cliente == null){
            flash.addFlashAttribute("error", "El cliente no existe en la base de datos");
            return "redirect:/listar";
        }

        model.put("cliente", cliente);
        model.put("titulo", "Detalles del cliente: " + cliente.getNombre() + " " + cliente.getApellido());

        return "ver";
    }

    // primero se especifica la ruta, y despues el motodo que por defecto es GET
    @RequestMapping(value = "/listar", method = RequestMethod.GET)
    public String listar(@RequestParam(name = "page", defaultValue = "0") int page, Model model){

        Pageable pageRequest = PageRequest.of(page, 5);

        Page<Cliente> clientes = clienteService.findAll(pageRequest);

        // creamos el objeto del paginador con su url y despues lo pasamos a la vista
        PageRender<Cliente> pageRender = new PageRender<>("/listar", clientes);

        model.addAttribute("titulo", "Listado de clientes");
        model.addAttribute("clientes", clientes);
        model.addAttribute("page", pageRender);

        return "listar";
    }

    @RequestMapping(value = "/form")
    public String crear(Map<String, Object> model){

        Cliente cliente = new Cliente();
        model.put("cliente", cliente);

        model.put("titulo", "Formulario de cliente");

        return "form";
    }

    @RequestMapping(value = "/form/{id}")
    public String editar(@PathVariable(value = "id") Long id, Map<String, Object> model, RedirectAttributes flash){

        Cliente cliente = null;
        if(id > 0){
            // si el id es mayor a 0 lo busca la clase Dao
            cliente = clienteService.findOne(id);

            if(cliente == null){
                flash.addFlashAttribute("error", "El ID no existe en la base de datos");
                return "redirect:/listar";
            }
        }else{
            flash.addFlashAttribute("error", "El ID del cliente no puede ser cero!");
            return "redirect:/listar";
        }

        // pasa a la vista el cliente correspondiente al id
        model.put("cliente", cliente);
        model.put("titulo", "Editar cliente");

        return "form";
    }

    @RequestMapping(value = "/form", method = RequestMethod.POST)
    public String guardar(@Valid Cliente cliente, BindingResult result,
                          Model model, @RequestParam("file") MultipartFile foto,
                          RedirectAttributes flash, SessionStatus sesion) {

        // verifica si hay errores, si los hay muestra un msj para que el usuario los corrija
        // sino guarda al cliente y retorna listar
        if(result.hasErrors()){
            model.addAttribute("titulo", "Formulario de cliente");
            return "form";
        }

        // verifica que el string de la foto no está vacío
        if(!foto.isEmpty()){

            // verificamos si el cliente existe,
            // en caso de que si se va a editar y cambiar la foto que tenia por la nueva
            if(cliente.getId() != null
                    && cliente.getId() > 0
                    && cliente.getFoto() != null
                    && cliente.getFoto().length() > 0){

               uploadFileService.delete(cliente.getFoto());
            }

            String uniqueFileName = null;

            try{
                //
                uniqueFileName = uploadFileService.copy(foto);
            }catch (IOException e){
                e.printStackTrace();
            }

            flash.addFlashAttribute("foto", "Se subio con exito " + "'" + uniqueFileName + "'");

            cliente.setFoto(uniqueFileName);

        }

        String mensajeFlash = (cliente.getId() != null)? "Cliente editado con éxito" : "Cliente creado con éxito";

        // recibe el objeto cliente y lo guarda
        clienteService.save(cliente);

        // elimina el objeto cliente de la sesion
        sesion.setComplete();
        flash.addFlashAttribute("success", mensajeFlash);
        return "redirect:listar";
    }

    @RequestMapping(value = "/eliminar/{id}")
    public String eliminar(@PathVariable(value = "id") Long id, RedirectAttributes flash){

        if(id > 0){
            // busca el cliente y lo elimina
            Cliente cliente = clienteService.findOne(id);

            clienteService.delete(id);

            flash.addFlashAttribute("error", "Cliente eliminado con éxito");

            if(uploadFileService.delete(cliente.getFoto())){
                flash.addFlashAttribute("error", "Foto " + cliente.getFoto() + " eliminada con exito!");
            }

        }

        return "redirect:/listar";
    }
}
