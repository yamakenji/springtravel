package com.example.samuraitravel.controller;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.samuraitravel.entity.House;
import com.example.samuraitravel.form.HouseEditForm;
import com.example.samuraitravel.form.HouseRegisterForm;
import com.example.samuraitravel.service.HouseService;

@Controller
@RequestMapping("/admin/houses")
public class AdminHouseController {
    private final HouseService houseService;

    public AdminHouseController(HouseService houseService) {
        this.houseService = houseService;
    }

    @GetMapping
    public String index(@RequestParam(name = "keyword", required = false) String keyword,
                        @PageableDefault(page = 0, size = 10, sort = "id", direction = Direction.ASC) Pageable pageable,
                        Model model)
    {
        Page<House> housePage;

        if (keyword != null && !keyword.isEmpty()) {
            housePage = houseService.findHousesByNameLike(keyword, pageable);
        } else {
            housePage = houseService.findAllHouses(pageable);
        }

        model.addAttribute("housePage", housePage);
        model.addAttribute("keyword", keyword);

        return "admin/houses/index";
    }

    @GetMapping("/{id}")
    public String show(@PathVariable(name = "id") Integer id, RedirectAttributes redirectAttributes, Model model) {
        Optional<House> optionalHouse  = houseService.findHouseById(id);

        if (optionalHouse.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "民宿が存在しません。");

            return "redirect:/admin/houses";
        }

        House house = optionalHouse.get();
        model.addAttribute("house", house);

        return "admin/houses/show";
    }

    @GetMapping("/register")
    public String register(Model model) {
        model.addAttribute("houseRegisterForm", new HouseRegisterForm());

        return "admin/houses/register";
    }

    @PostMapping("/create")
    public String create(@ModelAttribute @Validated HouseRegisterForm houseRegisterForm,
                         BindingResult bindingResult,
                         RedirectAttributes redirectAttributes,
                         Model model)
    {
        if (bindingResult.hasErrors()) {
            model.addAttribute("houseRegisterForm", houseRegisterForm);

            return "admin/houses/register";
        }

        houseService.createHouse(houseRegisterForm);
        redirectAttributes.addFlashAttribute("successMessage", "民宿を登録しました。");

        return "redirect:/admin/houses";
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable(name = "id") Integer id, RedirectAttributes redirectAttributes, Model model) {
        Optional<House> optionalHouse = houseService.findHouseById(id);

        if (optionalHouse.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "民宿が存在しません。");

            return "redirect:/admin/houses";
        }

        House house = optionalHouse.get();
        HouseEditForm houseEditForm = new HouseEditForm(house.getName(), null, house.getDescription(), house.getPrice(), house.getCapacity(), house.getPostalCode(), house.getAddress(), house.getPhoneNumber());

        model.addAttribute("house", house);
        model.addAttribute("houseEditForm", houseEditForm);

        return "admin/houses/edit";
    }

    @PostMapping("/{id}/update")
    public String update(@ModelAttribute @Validated HouseEditForm houseEditForm,
                         BindingResult bindingResult,
                         @PathVariable(name = "id") Integer id,
                         RedirectAttributes redirectAttributes,
                         Model model)
    {
        Optional<House> optionalHouse = houseService.findHouseById(id);

        if (optionalHouse.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "民宿が存在しません。");

            return "redirect:/admin/houses";
        }

        House house = optionalHouse.get();

        if (bindingResult.hasErrors()) {
            model.addAttribute("house", house);
            model.addAttribute("houseEditForm", houseEditForm);

            return "admin/houses/edit";
        }

        houseService.updateHouse(houseEditForm, house);
        redirectAttributes.addFlashAttribute("successMessage", "民宿情報を編集しました。");

        return "redirect:/admin/houses";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable(name = "id") Integer id, RedirectAttributes redirectAttributes) {
        Optional<House> optionalHouse = houseService.findHouseById(id);

        if (optionalHouse.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "民宿が存在しません。");

            return "redirect:/admin/houses";
        }

        House house = optionalHouse.get();
        houseService.deleteHouse(house);
        redirectAttributes.addFlashAttribute("successMessage", "民宿を削除しました。");

        return "redirect:/admin/houses";
    }
}
