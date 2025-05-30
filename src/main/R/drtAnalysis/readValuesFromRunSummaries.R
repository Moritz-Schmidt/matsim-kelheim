library(dplyr)
library(readr)
library(tidyr)
library(ggplot2)  # Für das Plotting mit ggplot2
library(plotly)

# Funktion zum Extrahieren der Parameter aus dem Ordnernamen
extract_parameters <- function(folder_name, speed) {
  # Extrahiere 'area'
  area <- strsplit(folder_name, '-')[[1]][1]
  
  # Extrahiere 'fleetSize'
  fleet_size_match <- regmatches(folder_name, regexpr("AV\\d+", folder_name))
  fleet_size <- as.numeric(gsub("AV", "", fleet_size_match))
  
  # Setze 'intermodal'
  intermodal <- grepl("intermodal", folder_name)
  
  # Setze 'allDay'
  all_day <- grepl("allDay", folder_name)
  
  return(list(speed = speed, area = area, fleetSize = fleet_size, intermodal = intermodal, allDay = all_day))
}

# Funktion zum Einlesen der CSV-Datei und Extrahieren der "mean"-Werte
read_stats <- function(folder_path, file_name, stats_for_AV) {
  if (stats_for_AV){
    folder_name <- "analysis/drt-drt-av"
  } else {
    folder_name <- "analysis/drt-drt"
  }
  
  csv_path <- file.path(folder_path, folder_name, file_name)
  
  if (file.exists(csv_path)) {
    print(paste(Sys.time(), ":  ", "reading", csv_path))
    df <- read_csv(csv_path, show_col_types = FALSE)
    mean_values <- df %>% select(parameter, mean)
    return(mean_values)
  } else {
    print(paste(Sys.time(), ":  ", "could not find ", csv_path))
    return(NULL)
  }
}

# Hauptfunktion zum Iterieren durch Unterordner
process_folders <- function(main_folder, speed, stats_for_AV) {
  # Liste aller Unterordner im Hauptordner
  subfolders <- list.dirs(main_folder, recursive = FALSE, full.names = FALSE)
  
  # Initialisiere eine Liste zum Speichern der Ergebnisse
  results <- list()
  
  # Iteriere durch alle Unterordner
  for (subfolder in subfolders) {
    parameters <- extract_parameters(subfolder, speed)
    full_path <- file.path(main_folder, subfolder)
    
    demand_mean_values <- read_stats(full_path, "avg_demand_stats.csv", stats_for_AV)
    supply_mean_values <- read_stats(full_path, "avg_supply_stats.csv", stats_for_AV)
    
    if (!is.null(demand_mean_values) || !is.null(supply_mean_values)) {
      if (!is.null(demand_mean_values)) {
        demand_mean_values <- demand_mean_values %>% 
          mutate(type = "demand",
                 speed = parameters$speed,
                 area = parameters$area,
                 fleetSize = parameters$fleetSize,
                 intermodal = parameters$intermodal,
                 allDay = parameters$allDay)
      }
      
      if (!is.null(supply_mean_values)) {
        supply_mean_values <- supply_mean_values %>% 
          mutate(type = "supply",
                 speed = parameters$speed,
                 area = parameters$area,
                 fleetSize = parameters$fleetSize,
                 intermodal = parameters$intermodal,
                 allDay = parameters$allDay)
      }
      
      combined_values <- bind_rows(demand_mean_values, supply_mean_values)
      results[[subfolder]] <- combined_values
    }
  }
  
  # Kombiniere alle Ergebnisse in eine Tabelle
  final_result <- bind_rows(results)
  return(final_result)
}

#############

mainDir <- "E:/matsim-kelheim/v3.1.1/output-KEXI-2.45-AV--0.0/"
speeds <- list(3.3, 5, 8.3)

stats_for_AV = TRUE #set to true for AV and FALSE for conv. KEXI

results <- list()
for (speed in speeds) {
  main_folder <- paste(mainDir, "AV-speed-mps-", speed, sep="")
  runResults <- process_folders(main_folder, speed, stats_for_AV)  
  results[[speed]] <- runResults
}

results <- bind_rows(results)

#####
# Transponiere die Tabelle, um Parameter als Spalten zu setzen
transposed_result <- results %>%
  select(speed, area, fleetSize, intermodal, allDay, parameter, mean) %>%
  spread(key = parameter, value = mean)

if (stats_for_AV){
  ###
  #in Realität haben wir eine avg gruppengr0eße von 1.7 gemessen, diese aber nicht simuliert.
  # wir rechnen die jetzt im nachhinein wieder drauf.
  transposed_result <- transposed_result %>% 
    mutate(`Passengers (Pax)` = `Handled Requests` * 1.7,
           `Total pax distance [km]` = `Total pax distance [km]` * 1.7) %>% 
    mutate(`Pax per veh` = `Passengers (Pax)` / Vehicles,
           `Pax per veh-km` = `Passengers (Pax)` / `Total vehicle mileage [km]`,
           `Pax per veh-h` = `Passengers (Pax)` / `Total service hours`,
           `Occupancy rate [pax-km/v-km]` = `Total pax distance [km]` / `Total vehicle mileage [km]`)
}

#transponiere zurück
results <- transposed_result %>%
  gather(key = "parameter", value = "mean", -speed, -area, -fleetSize, -intermodal, -allDay)

# Ergebnisse ausgeben
print(results)
print(transposed_result)

if (stats_for_AV){
  output_file <- "results-av.csv"
} else {
  output_file <- "results-konvKEXI.csv"
}
write_csv(transposed_result, paste(mainDir, output_file, sep=""))

#####################################################################
######PLOTS####

### !!! the plot for the final project report in German
### are produced with plotRunSummaries.R !!!

plotByConfiguration <- function(parameterStr){
  
  # Filtern der Daten für die gewünschten Parameter
  plot_data <- results %>%
    filter(parameter == parameterStr,
           intermodal == TRUE | area == "SAR2023")
           #area == "WIEKEXImSaal")
  
  # Funktion zum Anpassen der Facet-Labels
  label_function <- function(value) {
    paste(value, "m/s")
  }
  
  # Erstellen des Facet-Plots
  ggplot(plot_data, aes(x = fleetSize, y = mean, color = area, linetype = as.factor(allDay), group = interaction(area, allDay))) +
    geom_line(linewidth = 1.2) +
    geom_point(size = 3,
               #aes(shape = as.factor(intermodal))
               ) +
    facet_wrap(~ speed,
               labeller = labeller(speed = label_function)
               ,scales = "fixed"
               ) +
    geom_text(aes(label = fleetSize), vjust = -1, hjust = 0.5, size = 3, color = "black") +
    labs(title = paste(parameterStr, "by Fleet Size, Speed, Area and Service Hours"),
         x = "Fleet Size",
         y = parameterStr,
         color = "Area",
         linetype = "All Day"
         #,shape = "Intermodal"
         ) +
    #theme_dark() +
    theme(
      plot.title = element_text(size = 16, face = "bold"),  # Titelgröße anpassen
      axis.title.x = element_text(size = 14),  # X-Achsentitelgröße anpassen
      axis.title.y = element_text(size = 14),  # Y-Achsentitelgröße anpassen
      axis.text = element_text(size = 12),  # Achsentextgröße anpassen
      legend.title = element_text(size = 14),  # Legendentitelgröße anpassen
      legend.text = element_text(size = 12),  # Legendtextgröße anpassen
      strip.text = element_text(size = 12)  # Facet-Textgröße anpassen
    )
  
}

#unique(results$parameter)
plotByConfiguration("Handled Requests")
plotByConfiguration("Avg. wait time")
plotByConfiguration("Avg. ride distance [km]")
plotByConfiguration("Empty ratio")
plotByConfiguration("Total vehicle mileage [km]")
plotByConfiguration("Avg. fare [MoneyUnit]" )
plotByConfiguration("Pax per veh-km")

  
#####################
##Zusammenhang wait time und Nachfrage

  handled_requests_data <- results %>%
    filter(parameter == "Handled Requests") %>%
    select(area, speed, fleetSize, allDay, mean, intermodal) %>%
    rename(handled_requests = mean)
  
  avg_wait_time_data <- results %>%
    filter(parameter == "Avg. wait time") %>%
    select(area, speed, fleetSize, allDay, mean, intermodal) %>%
    rename(avg_wait_time = mean)
  
  # Zusammenführen der Daten
  plot_data <- left_join(handled_requests_data, avg_wait_time_data, by = c("area", "speed", "fleetSize", "allDay", "intermodal"))
  
  # Erstellen des Facet-Plots
  facet_plot <- ggplot(plot_data, aes(x = avg_wait_time, y = handled_requests, color = area, linetype = as.factor(allDay), group = interaction(area, allDay))) +
    geom_line(linewidth = 1.2) +
    geom_point(size = 3
               #,aes(shape = as.factor(intermodal))
               ) +
    facet_wrap(~ speed
               #, scales = "free"
               ) +
    labs(title = "Handled Requests by Avg. Wait Time, Speed, Area, and All Day",
         x = "Avg. Wait Time",
         y = "Handled Requests",
         color = "Area",
         linetype = "All Day"
         #,shape = "Intermodal"
         ) +
    theme_dark() +
    theme(
      plot.title = element_text(size = 16, face = "bold"),  # Titelgröße anpassen
      axis.title.x = element_text(size = 14),  # X-Achsentitelgröße anpassen
      axis.title.y = element_text(size = 14),  # Y-Achsentitelgröße anpassen
      axis.text = element_text(size = 12),  # Achsentextgröße anpassen
      legend.title = element_text(size = 14),  # Legendentitelgröße anpassen
      legend.text = element_text(size = 12),  # Legendtextgröße anpassen
      strip.text = element_text(size = 12)  # Facet-Textgröße anpassen
    )
  
  # Plot anzeigen
  print(facet_plot)
    
  