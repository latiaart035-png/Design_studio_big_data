// com.designstudio.infrastructure.config.DataInitializer.java (обновлённая версия)
package com.designstudio.infrastructure.config;

import com.designstudio.domain.entity.CustomerSegment;
import com.designstudio.domain.entity.CampaignTemplate;
import com.designstudio.domain.entity.ProductCategory;
import com.designstudio.domain.entity.User;
import com.designstudio.domain.repository.CustomerSegmentRepository;
import com.designstudio.domain.repository.CampaignTemplateRepository;
import com.designstudio.domain.repository.ProductCategoryRepository;
import com.designstudio.domain.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final ProductCategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final CustomerSegmentRepository segmentRepository;
    private final CampaignTemplateRepository templateRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(ProductCategoryRepository categoryRepository,
                           UserRepository userRepository,
                           CustomerSegmentRepository segmentRepository,
                           CampaignTemplateRepository templateRepository,
                           PasswordEncoder passwordEncoder) {
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.segmentRepository = segmentRepository;
        this.templateRepository = templateRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        initCategories();
        initAdmin();
        initSegments();
        initTemplates();
    }

    private void initCategories() {
        if (categoryRepository.count() > 0) return;

        String[] names = {"Гостиная", "Спальня", "Кухня", "Ванная", "Офис"};
        for (String name : names) {
            ProductCategory cat = new ProductCategory();
            cat.setName(name);
            categoryRepository.save(cat);
        }
    }

    private void initAdmin() {
        if (userRepository.findByEmail("admin@designstudio.ru") != null) return;

        User admin = new User();
        admin.setFirstName("Администратор");
        admin.setLastName("Системы");
        admin.setEmail("admin@designstudio.ru");
        admin.setPassword(passwordEncoder.encode("123"));
        admin.setRole(User.Role.ADMIN);
        admin.setStatus(User.Status.VIP);
        admin.setDiscount(0.15);
        userRepository.save(admin);
    }

    private void initSegments() {
        if (segmentRepository.count() > 0) return;

        // VIP сегмент
        CustomerSegment vip = new CustomerSegment();
        vip.setName("VIP");
        vip.setDescription("Ключевые клиенты с максимальными тратами и лояльностью");
        vip.setMinSpent(50000.0);
        vip.setStatusRequirement("VIP");
        vip.setColor("#28a745");
        vip.setIcon("bi-star-fill");
        segmentRepository.save(vip);

        // Premium сегмент
        CustomerSegment premium = new CustomerSegment();
        premium.setName("PREMIUM");
        premium.setDescription("Постоянные клиенты с регулярными покупками");
        premium.setMinSpent(20000.0);
        premium.setStatusRequirement("PREMIUM");
        premium.setColor("#17a2b8");
        premium.setIcon("bi-star-half");
        segmentRepository.save(premium);

        // Active сегмент
        CustomerSegment active = new CustomerSegment();
        active.setName("ACTIVE");
        active.setDescription("Активные клиенты с текущими покупками");
        active.setMinOrders(1);
        active.setColor("#ffc107");
        active.setIcon("bi-activity");
        segmentRepository.save(active);

        // New сегмент
        CustomerSegment newSeg = new CustomerSegment();
        newSeg.setName("NEW");
        newSeg.setDescription("Новые клиенты, недавно зарегистрированные");
        newSeg.setActivityDays(30);
        newSeg.setColor("#6c757d");
        newSeg.setIcon("bi-person-plus");
        segmentRepository.save(newSeg);

        // Lost сегмент
        CustomerSegment lost = new CustomerSegment();
        lost.setName("LOST");
        lost.setDescription("Потерянные клиенты, не совершали покупок более 180 дней");
        lost.setActivityDays(180);
        lost.setColor("#dc3545");
        lost.setIcon("bi-emoji-dizzy");
        segmentRepository.save(lost);
    }

    private void initTemplates() {
        if (templateRepository.count() > 0) return;

        // Приветственный шаблон
        CampaignTemplate welcome = new CampaignTemplate();
        welcome.setName("Приветственное письмо");
        welcome.setDescription("Добро пожаловать в Design Studio!");
        welcome.setType(CampaignTemplate.TemplateType.EMAIL);
        welcome.setCategory("welcome");
        welcome.setSubjectTemplate("Добро пожаловать, {name}! 🎉");
        welcome.setContentTemplate("Здравствуйте, {name}!\n\n" +
                "Рады приветствовать вас в Design Studio!\n\n" +
                "Мы подготовили для вас скидку {discount}% на первый заказ.\n\n" +
                "Используйте код WELCOME при оформлении заказа.\n\n" +
                "С уважением,\nКоманда Design Studio");
        templateRepository.save(welcome);

        // Промо-шаблон
        CampaignTemplate promo = new CampaignTemplate();
        promo.setName("Скидка {discount}%");
        promo.setDescription("Стандартное промо-письмо со скидкой");
        promo.setType(CampaignTemplate.TemplateType.EMAIL);
        promo.setCategory("promo");
        promo.setSubjectTemplate("Только для вас: скидка {discount}%!");
        promo.setContentTemplate("Здравствуйте, {name}!\n\n" +
                "Мы подготовили специальное предложение для вас!\n\n" +
                "Скидка {discount}% действует до конца недели.\n\n" +
                "Успейте сделать заказ и сэкономить!\n\n" +
                "С уважением,\nКоманда Design Studio");
        templateRepository.save(promo);

        // Реактивационный шаблон
        CampaignTemplate reactivate = new CampaignTemplate();
        reactivate.setName("Мы скучаем!");
        reactivate.setDescription("Возврат потерянных клиентов");
        reactivate.setType(CampaignTemplate.TemplateType.EMAIL);
        reactivate.setCategory("reactivation");
        reactivate.setSubjectTemplate("Мы скучаем по вам, {name}!");
        reactivate.setContentTemplate("Здравствуйте, {name}!\n\n" +
                "Мы заметили, что давно не виделись...\n\n" +
                "Вернитесь с большой скидкой {discount}%!\n\n" +
                "Это наш подарок за то, что вы были с нами раньше.\n\n" +
                "С возвращением,\nКоманда Design Studio");
        templateRepository.save(reactivate);
    }
}